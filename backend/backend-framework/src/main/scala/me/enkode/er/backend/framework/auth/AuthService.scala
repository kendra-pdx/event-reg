package me.enkode.er.backend.framework.auth

import java.time.Instant
import java.util.Base64

import cats._
import cats.data.Validated._
import cats.data._
import cats.implicits._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.util.Try

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

  case object AlwaysFail extends ValidationError

  def sign(header: String, body: String, key: Key): String = {
    val alg = "HmacSHA256"
    val hmacSHA256 = Mac.getInstance(alg)
    val keySpec = new SecretKeySpec(key.data, alg)
    hmacSHA256.init(keySpec)
    Base64.getEncoder.encodeToString(hmacSHA256.doFinal(List(header, body).mkString(".").getBytes()))
  }
}

class AuthService[F[_]: MonadError[*[_], Throwable]](keyRepository: KeyRepository[F]) {
  import AuthService._

  private def keyIdFor(authInfo: AuthInfo): F[KeyId] = {
    authInfo.issuer match {
      case  AuthInfo.IssuerEncoding(_, _, keyId) => keyId.pure[F]
      case  _ => KeyExtractionException.raiseError[F, KeyId]
    }
  }

  type ValidationResult[T] = ValidatedNel[ValidationError, T]

  private def validateAuthInfo(authInfo: AuthInfo): F[ValidationResult[AuthInfo]] = {

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
        "scp" -> authInfo.scopes.map(s => Str(s.asString))
      )))

      val signature = sign(header, body, key)

      AuthToken(header, body, signature)
    }
  }

  def renderToken(authToken: AuthToken): String = {
    List(authToken.header, authToken.body, authToken.signature).mkString(".")
  }

  def parseJwt(jwt: String): AuthToken = {
    val List(header, body, signature) = jwt.split('.').toList
    AuthToken(header, body, signature)
  }
}
