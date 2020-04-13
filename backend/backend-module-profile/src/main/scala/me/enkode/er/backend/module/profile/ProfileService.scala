package me.enkode.er.backend.module.profile

import java.security.MessageDigest

import cats._
import cats.data.EitherT
import cats.implicits._
import me.enkode.er.backend.framework.auth.{AuthInfo, AuthService}

import scala.concurrent.duration._

object ProfileService {
  sealed trait LoginFailure
  case class InvalidLogin(email: String) extends LoginFailure

  def hash(clear: String): Array[Byte] = {
    val salty = s"me.enkode.profile.pw:$clear"
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(salty.getBytes)
  }
}

class ProfileService[F[_]: MonadError[*[_], Throwable]](
  profileRepository: ProfileRepository[F],
  authService: AuthService[F],
) {
  import ProfileService._

  def login(email: String, password: String): F[Either[LoginFailure, Login]] = {
    val maybeUser: F[Either[LoginFailure, User]] = for {
      user <- profileRepository.findUserByEmail(email)
    } yield {
      user.fold(InvalidLogin(email).asLeft[User]) { user =>
        if (user.password.hash sameElements hash(password)) {
          user.asRight[InvalidLogin]
        } else {
          InvalidLogin(email).asLeft[User]
        }
      }
    }

    val userScopes: List[AuthInfo.Scope] = List(AuthInfo.Scope("*"))
    val refreshScopes: List[AuthInfo.Scope] = List(AuthInfo.Scope("refresh"))

    (for {
      user <- EitherT(maybeUser)
      userSubject = AuthInfo.Subject(user.profile.profileId.asString)
      authToken <- EitherT(authService.createToken(userSubject, scopes = userScopes)
        .map(_.asRight[LoginFailure]))
      refreshToken <- EitherT(authService.createToken(userSubject, scopes = refreshScopes, duration = 15.days)
        .map(_.asRight[LoginFailure]))
    } yield {
      Login(user, authToken, refreshToken)
    }).value
  }

}
