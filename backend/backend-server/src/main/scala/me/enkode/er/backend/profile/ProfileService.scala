package me.enkode.er.backend.profile

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

import cats._
import cats.data.EitherT
import cats.implicits._
import me.enkode.er.backend.auth.{AuthInfo, AuthService}
import me.enkode.er.backend.framework.log.{ConsoleLogger, TraceSpan}

import scala.concurrent.duration._
import scala.util.control.NonFatal

object ProfileService {
  sealed trait LoginFailure
  case class InvalidLogin(email: String) extends LoginFailure

  sealed trait CreateUserFailure
  case class DuplicateUser(user: User) extends CreateUserFailure

  sealed trait FindProfileFailure
  case class ProfileNotFound(profileId: ProfileId) extends FindProfileFailure
  case class FindProfileRepFailure(t: Throwable) extends FindProfileFailure

  def hash(clear: String, unique: String): Array[Byte] = {
    val salty = s"me.enkode.profile.pw:$unique:$clear"
    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(salty.getBytes)
  }
}

class ProfileService[F[_]: MonadError[*[_], Throwable]](
  profileRepository: ProfileRepository[F],
  authService: AuthService[F],
) {
  import ProfileService._
  val logger = new ConsoleLogger(classOf[ProfileService[F]].getSimpleName, ConsoleLogger.Level.Debug)

  def login(email: String, password: String): F[Either[LoginFailure, Login]] = {
    val maybeUser: F[Either[LoginFailure, User]] = for {
      user <- profileRepository.findUserByEmail(email)
    } yield {
      user.fold(InvalidLogin(email).asLeft[User]) { user =>
        if (user.password.hash sameElements hash(password, email)) {
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

  def createUser[Ctx: TraceSpan.Container](email: String, fullName: String, clearPassword: String)(implicit C: Ctx): F[Either[CreateUserFailure, User]] = {
    //todo: validate inputs
    val password = Password(hash(clearPassword, email), Instant.now)
    val user = User(
      Profile(ProfileId(UUID.randomUUID().toString), fullName, email)
    )(password)
    (for {
      user <- profileRepository.insert(user)
    } yield {
      user.asRight[CreateUserFailure]
    }).recover({
      case ProfileRepository.DuplicateUserError(_) =>
        DuplicateUser(user).asLeft[User]

      case NonFatal(t) =>
        logger.error(s"create user failed: $t")
        throw t
    })
  }

  def findProfileById(id: ProfileId): F[Either[FindProfileFailure, Profile]] = {
    (for {
      profile <- profileRepository.findProfileById(UUID.fromString(id.asString))
    } yield {
      lazy val notFound: FindProfileFailure = ProfileNotFound(id)
      profile.fold(notFound.asLeft[Profile])(_.asRight[FindProfileFailure])
    }).recover({
      case t: Throwable => FindProfileRepFailure(t).asLeft[Profile]
    })
  }
}
