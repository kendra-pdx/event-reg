package me.enkode.er.backend.profile

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import cats.data.EitherT
import cats.implicits._
import me.enkode.er.backend.auth.AuthInfo.{Scope, Subject}
import me.enkode.er.backend.auth.{AuthDirectives, AuthService}
import me.enkode.er.backend.framework.log._
import me.enkode.er.backend.framework.{Endpoint, ErrorResponse, μPickleMarshallingSupport}

import scala.concurrent.{ExecutionContext, Future}

class ProfileEndpoint(
  profileService: ProfileService[Future],
  val authService: AuthService[Future]
)(implicit ec: ExecutionContext) extends Endpoint with AuthDirectives with μPickleMarshallingSupport {
  val logger = new ConsoleLogger(getClass.getSimpleName, ConsoleLogger.Level.Debug)
  override val name: String = "profile"

  import AuthDirectives._
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
            logger.info(s"creating profile: email=${createUser}")
            (for {
              created <- EitherT(profileService.createUser(createUser.email, createUser.fullName, createUser.password))
            } yield {
              CreateUserResponse(created.profile.email, created.profile.email, created.profile.fullName)
            }).valueOr({
              case ProfileService.DuplicateUser(_) => throw ErrorResponse.ClientError(s"user already exists")
            })
          }
        }
      }
    }

    val getProfile = path(basePathMatcher / "profile" / MatchProfileId) { profileId =>
      requestTrace("getProfile") { implicit traceSpan: TraceSpan =>
        get {
          requireAuth(hasAllScopes(Scope("*")), subjectIs(Subject(profileId.asString)))(traceSpan) { _ =>
            logger.debug(Map("profileId" -> profileId.asString))
            complete {
              (for {
                profile <- EitherT(profileService.findProfileById(profileId))
              } yield {
                ProfileResponse(profile)
              }).valueOr({
                case ProfileService.ProfileNotFound(id) => throw ErrorResponse.ClientError(s"$id not found")
                case ProfileService.FindProfileRepFailure(t) => throw ErrorResponse.ClientError(t.getMessage)
              })
            }
          }
        }
      }
    }

    val login = path(basePathMatcher / "login") {
      (post & entity(as[LoginRequest])) { loginRequest =>
        requestTrace("login") { implicit traceSpan =>
          complete {
            logger.debug(s"login: email=${loginRequest.email}")
            (for {
              login <- EitherT(profileService.login(loginRequest.email, loginRequest.password))
              profileId = login.user.profile.profileId
              authToken = authService.renderToken(login.authToken)
              refreshToken = authService.renderToken(login.refreshToken)
            } yield {
              LoginResponse(profileId.asString, authToken, refreshToken)
            }).valueOr({
              case ProfileService.InvalidLogin(invalidEmail) =>
                throw ErrorResponse.ClientError(s"invalid login: $invalidEmail")
            })
          }
        }
      }
    }

    createProfile ~ getProfile ~ login
  }
}
