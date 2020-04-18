package me.enkode.er.backend.auth

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.{Base64, UUID}

import cats._
import cats.data.Validated._
import cats.data._
import cats.implicits._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.concurrent.duration._
import scala.util.{Random, Try}

object AuthService {
  private case object KeyExtractionException extends RuntimeException("token did not contain a key id")

  sealed trait ValidationError
  case object NoKeyId extends ValidationError
  case object InvalidKey extends ValidationError
  case object KeyOutOfDate extends ValidationError
  case object InvalidBase64Encoding extends ValidationError
  case object InvalidJsonEncoding extends ValidationError
  case class MissingField(name: String) extends ValidationError
  case class InvalidField(name: String) extends ValidationError
  case object InvalidSignature extends ValidationError

  type ValidationResult[T] = ValidatedNel[ValidationError, T]

  def sign(header: String, body: String, key: Key): String = {
    val alg = "HmacSHA256"
    val hmacSHA256 = Mac.getInstance(alg)
    val keySpec = new SecretKeySpec(key.data, alg)
    hmacSHA256.init(keySpec)
    Base64.getEncoder.encodeToString(hmacSHA256.doFinal(List(header, body).mkString(".").getBytes()))
  }
}

class AuthService[F[_]: MonadError[*[_], Throwable]](
  keyRepository: KeyRepository[F],
  audience: AuthInfo.Audience = AuthInfo.Audience("*")
) {
  import AuthService._

  private def keyIdFor(authInfo: AuthInfo): F[KeyId] = {
    authInfo.issuer match {
      case  AuthInfo.IssuerEncoding(_, _, keyId) => keyId.pure[F]
      case  _ => KeyExtractionException.raiseError[F, KeyId]
    }
  }

  private def validateAuthInfo(authInfo: AuthInfo): F[ValidationResult[AuthInfo]] = {
    //todo: validate exp / nbf
    //todo: validate aud
    authInfo.validNel[ValidationError].pure[F]
  }

  private def validateTokenSignature(authToken: AuthToken, key: Key): ValidationResult[AuthToken]= {
    Either.cond(
      sign(authToken.header, authToken.body, key) == authToken.signature,
      authToken,
      InvalidSignature
    ).toValidatedNel
  }

  private def validateBase64Decode(encoded: String): ValidationResult[String] = {
    try {
      new String(Base64.getDecoder.decode(encoded)).validNel[ValidationError]
    } catch {
      case _: Throwable => InvalidBase64Encoding.invalidNel[String]
    }
  }

  private def validateTokenBodyParse(tokenBody: String): ValidationResult[ujson.Obj] = {
    import upickle.default._
    try {
      read[ujson.Obj](tokenBody).validNel[ValidationError]
    } catch {
      case _: Throwable => InvalidJsonEncoding.invalidNel[ujson.Obj]
    }
  }

  private def validateExtractAuthInfo(body: ujson.Obj): ValidationResult[AuthInfo] = {
    def field[T](name: String, as: ujson.Value => T, asE: => ValidationError): ValidationResult[T] = {
      body.obj.get(name)
        .fold((MissingField(name): ValidationError).invalidNel[T])({v =>
           Try(as(v).validNel[ValidationError]).getOrElse(asE.invalidNel[T])
        })
    }

    (
      field("iss", v => AuthInfo.Issuer(v.str), InvalidField("iss")),
      field("sub", v => AuthInfo.Subject(v.str), InvalidField("sub")),
      field("aud", v => AuthInfo.Audience(v.str), InvalidField("aud")),
      field("exp", v => AuthInfo.Expires(Instant.ofEpochSecond(v.num.toLong)), InvalidField("exp")),
      field("nbf", v => AuthInfo.NotBefore(Instant.ofEpochSecond(v.num.toLong)), InvalidField("nbf")),
      field("iat", v => AuthInfo.IssuedAt(Instant.ofEpochSecond(v.num.toLong)), InvalidField("iat")),
      field("jti", v => AuthInfo.JwtId(v.str), InvalidField("jti")),
      field("scp", v => v.arr.toList.map(_.str).map(AuthInfo.Scope), InvalidField("scp"))
    ).mapN(AuthInfo.apply)
  }

  private def validateKeyExpires(key: Key, now: Instant = Instant.now): ValidationResult[Key] = {
    Either.cond(
      key.expires.isAfter(now),
      key,
      KeyOutOfDate
    ).toValidatedNel
  }

  private def validateKeyNotBefore(key: Key, now: Instant = Instant.now): ValidationResult[Key] = {
    Either.cond(
      key.notBefore.isBefore(now),
      key,
      KeyOutOfDate
    ).toValidatedNel
  }

  private def validateKey(authInfo: AuthInfo): F[ValidationResult[Key]] = {
    (for {
      keyId <- keyIdFor(authInfo)
      key <- keyRepository.getKey(keyId)
    } yield {
      NonEmptyList.of(
        validateKeyExpires(key),
        validateKeyNotBefore(key),
      ).reduce((a: ValidationResult[Key], b: ValidationResult[Key]) => a *> b)
    }).recover({
      case KeyExtractionException => NoKeyId.invalidNel[Key]
      case KeyRepository.KeyNotFoundError(_) => InvalidKey.invalidNel[Key]
    })
  }

  /**
   * extracts the auth token, validating the information along the way
   * - that it is well formed
   * - that the key is usable and valid
   * - that the contents are allowed
   * - that the signature is valid
   * @param authToken the source token
   * @return validated authinfo
   */
  def validateAuthToken(authToken: AuthToken): F[ValidationResult[AuthInfo]] = {
    def validAuthInfo: F[ValidationResult[AuthInfo]] = validateBase64Decode(authToken.body)
      .andThen(validateTokenBodyParse)
      .andThen(validateExtractAuthInfo)
      .fold(_.invalid[AuthInfo].pure[F], validateAuthInfo)

    def validKey: F[ValidationResult[Key]] = validAuthInfo.flatMap(vAuthInfo => {
      vAuthInfo.fold(_.invalid[Key].pure[F], validateKey)
    })

    def validSignature: F[ValidationResult[AuthToken]] = for {
      vKey <- validKey
    } yield {
      //todo: this duplicates failures from the key check in combination with *> below
      vKey.fold(_.invalid[AuthToken], k => validateTokenSignature(authToken, k))


    }

    for {
      vAuthInfo <- validAuthInfo // to map back to auth info
      vSignature <- validSignature // contains all errors
    } yield {
      vAuthInfo *> vAuthInfo.andThen(va => vSignature.map(_ => va))
    }
  }

  /**
   * generate an auth token from the desired auth info, using the key specified as part of the issuer
   * @param authInfo the source information to tokenize
   * @return the auth token
   */
  def authInfoToToken(authInfo: AuthInfo): F[AuthToken] = {
    import ujson._
    def base64e(src: String): String = {
      Base64.getEncoder.encodeToString(src.getBytes)
    }

    for {
      keyId <- keyIdFor(authInfo)
      key <- keyRepository.getKey(keyId)
    } yield {
      val header = base64e(write(Obj(
        "typ" -> Str("JWT"),
        "alg" -> "HS256")
      ))

      val body = base64e(write(Obj(
        "iss" -> Str(authInfo.issuer.asString),
        "sub" -> Str(authInfo.subject.asString),
        "aud" -> Str(authInfo.audience.asString),
        "exp" -> Num(authInfo.expires.asInstant.toEpochMilli / 1000),
        "nbf" -> Num(authInfo.notBefore.asInstant.toEpochMilli / 1000),
        "iat" -> Num(authInfo.issuedAt.asInstant.toEpochMilli / 1000),
        "jti" -> Str(authInfo.jwtId.asString),
        "scp" -> authInfo.scopes.map(s => Str(s.asString))
      )))

      val signature = sign(header, body, key)

      AuthToken(header, body, signature)
    }
  }

  /**
   * render's an AuthToken to a JWT string
   * @param authToken the components of the token
   * @return the JWT string representation of the token
   */
  def renderToken(authToken: AuthToken): String = {
    List(authToken.header, authToken.body, authToken.signature).mkString(".")
  }

  /**
   * parses a JWT representation of a token into a AuthToken
   * @param jwt the JWT string
   * @return the components of the token
   */
  def parseJwt(jwt: String): AuthToken = {
    //todo: make this more resilient
    val List(header, body, signature) = jwt.split('.').toList
    AuthToken(header, body, signature)
  }

  def createToken(
    subject: AuthInfo.Subject,
    duration: FiniteDuration = 1.hour,
    audience: AuthInfo.Audience = AuthService.this.audience,
    scopes: List[AuthInfo.Scope] = Nil,
  ): F[AuthToken] = {
    def issuerDomain = classOf[AuthService[F]].getName
    for {
      key <- keyRepository.currentKey()
      authInfo = AuthInfo(
        AuthInfo.IssuerEncoding(issuerDomain, key.keyId),
        subject,
        audience,
        AuthInfo.Expires(Instant.now.plus(Duration.ofNanos(duration.toNanos))),
        AuthInfo.NotBefore(Instant.now.minus(1, ChronoUnit.SECONDS)),
        AuthInfo.IssuedAt(Instant.now),
        AuthInfo.JwtId(UUID.randomUUID().toString),
        scopes
      )
      token <- authInfoToToken(authInfo)
    } yield {
      token
    }
  }

  def generateKey(duration: FiniteDuration = 365.days): F[Key] = {
    val now = Instant.now()
    val key = new Key {
      override val keyId: KeyId = KeyId(UUID.randomUUID().toString)
      override val keyType: KeyType = KeyType("shared")
      override val data: Array[Byte] = Random.alphanumeric.take(64).mkString.getBytes
      override val expires: Instant = now.plus(Duration.ofNanos(duration.toNanos))
      override val notBefore: Instant = now.minus(Duration.ofMinutes(1))
    }

    for {
      _ <- keyRepository.saveKey(key)
    } yield {
      key
    }
  }
}
