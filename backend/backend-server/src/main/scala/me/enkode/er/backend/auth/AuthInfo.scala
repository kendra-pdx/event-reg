package me.enkode.er.backend.auth

import java.time.Instant

object AuthInfo {
  case class Issuer(asString: String) extends AnyVal
  case class Subject(asString: String) extends AnyVal
  case class Audience(asString: String) extends AnyVal
  case class Expires(asInstant: Instant) extends AnyVal
  case class NotBefore(asInstant: Instant) extends AnyVal
  case class IssuedAt(asInstant: Instant) extends AnyVal
  case class JwtId(asString: String) extends AnyVal

  case class Scope(asString: String) extends AnyVal

  object IssuerEncoding {
    def apply(principle: String, keyId: KeyId): Issuer = {
      Issuer(s"urn:me.enkode.auth:$principle:${keyId.asString}")
    }

    def unapply(arg: Issuer): Option[(String, String, KeyId)] = {
      try {
        val Array(_, domain, principle, kid) = arg.asString.split(':')
        Some((domain, principle, KeyId(kid)))
      } catch {
        case _: Throwable => None // presumably couldn't split
      }
    }
  }
}

/**
 * a valid jwt-represented auth token for this system
 */
case class AuthInfo(
  issuer: AuthInfo.Issuer,
  subject: AuthInfo.Subject,
  audience: AuthInfo.Audience,
  expires: AuthInfo.Expires,
  notBefore: AuthInfo.NotBefore,
  issuedAt: AuthInfo.IssuedAt,
  jwtId: AuthInfo.JwtId,
  scopes: List[AuthInfo.Scope],
)