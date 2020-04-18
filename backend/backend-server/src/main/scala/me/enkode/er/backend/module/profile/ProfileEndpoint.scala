package me.enkode.er.backend.module.profile

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import me.enkode.er.backend.framework.auth.AuthService
import me.enkode.er.backend.framework.log._
import me.enkode.er.backend.framework.{Endpoint, μPickleMarshallingSupport}

import scala.concurrent.{ExecutionContext, Future}

class ProfileEndpoint(
  profileService: ProfileService[Future],
  authService: AuthService[Future]
)(implicit ec: ExecutionContext) extends Endpoint with μPickleMarshallingSupport {
  val logger = new ConsoleLogger(getClass.getSimpleName, ConsoleLogger.Level.Debug)
  override val name: String = "profile"

  import upickle.default._

  // --== GET PROFILE ==--
  case class ProfileResponse(profile: Profile)

  object ProfileResponse {
    implicit val ProfileIdRW: ReadWriter[ProfileId] =
      readwriter[String].bimap[ProfileId](_.asString, ProfileId)
    implicit val ProfileRW: ReadWriter[Profile] = macroRW
    implicit val ProfileResponseRW: ReadWriter[ProfileResponse] = macroRW
  }

  // --== LOGIN ==--

  case class LoginRequest(email: String, password: String)
  object LoginRequest {
    implicit val LoginRequestRW: ReadWriter[LoginRequest] = macroRW
  }

  case class LoginResponse(profileId: String, authToken: String, refreshToken: String)
  object LoginResponse {
    implicit val LoginResponseRW: ReadWriter[LoginResponse] = macroRW
  }

  // --== CREATE USER ==--
  case class CreateUserRequest(email: String, fullName: String, password: String)
  object CreateUserRequest {
    implicit val CreateUserRequestRW: ReadWriter[CreateUserRequest] = macroRW
  }

  case class CreateUserResponse(id: String, email: String, fullName: String)
  object CreateUserResponse {
    implicit val CreateUserResponseRW: ReadWriter[CreateUserResponse] = macroRW
  }

  // --== routes ==--

  private val MatchProfileId = JavaUUID.map(_.toString).map(ProfileId)

  override def createRoute(basePathMatcher: PathMatcher[Unit]): Route = {
    val createProfile = path(basePathMatcher / "profile") {
      requestTrace("createProfile") { implicit traceSpan: TraceSpan =>
        (post & entity(as[CreateUserRequest])) { createUser =>
          complete {
            logger.info(s"creating profile: email=${createUser.email}")
            (for {
              created <- EitherT(profileService.createUser(createUser.email, createUser.fullName, createUser.password))
            } yield {
              CreateUserResponse(created.profile.email, created.profile.email, created.profile.fullName)
            }).valueOr(err => throw new RuntimeException(err.toString))
          }
        }
      }
    }

    val getProfile = path(basePathMatcher / "profile" / MatchProfileId) { profileId =>
      requestTrace("getProfile") { implicit traceSpan: TraceSpan =>
        get {
          logger.debug(Map("profileId" -> profileId.asString))
          val profile = Profile(profileId, "Kendra Elford", "kendra@enkode.me")
          complete(ProfileResponse(profile))
        }
      }
    }

    val login = path(basePathMatcher / "login") {
      (post & entity(as[LoginRequest])) { loginRequest =>
        requestTrace("login") { implicit traceSpan =>
          complete {
            (for {
              login <- EitherT(profileService.login(loginRequest.email, loginRequest.password))
              profileId = login.user.profile.profileId
              authToken = authService.renderToken(login.authToken)
              refreshToken = authService.renderToken(login.refreshToken)
            } yield {
              LoginResponse(profileId.asString, authToken, refreshToken)
            }).valueOr(err => throw new RuntimeException(err.toString))
          }
        }
      }
    }

    createProfile ~ getProfile ~ login
  }
}
