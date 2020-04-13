package me.enkode.er.backend.module.profile

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import me.enkode.er.backend.framework.log._
import me.enkode.er.backend.framework.{Endpoint, μPickleMarshallingSupport}

import scala.concurrent.{ExecutionContext, Future}

class ProfileEndpoint(profileService: ProfileService[Future])(implicit ec: ExecutionContext) extends Endpoint with μPickleMarshallingSupport {
  val logger = new ConsoleLogger(getClass.getSimpleName, ConsoleLogger.Level.Debug)
  override val name: String = "profile"

  import upickle.default._

  case class ProfileResponse(profile: Profile)

  object ProfileResponse {
    implicit val ProfileIdRW: ReadWriter[ProfileId] =
      readwriter[String].bimap[ProfileId](_.asString, ProfileId)
    implicit val ProfileRW: ReadWriter[Profile] = macroRW
    implicit val ProfileResponseRW: ReadWriter[ProfileResponse] = macroRW
  }

  case class LoginRequest(login: String, password: String)
  object LoginRequest {
    implicit val LoginRequestRW: ReadWriter[LoginRequest] = macroRW
  }

  case class LoginResponse(authToken: String, refreshToken: String)

  private val MatchProfileId = JavaUUID.map(_.toString).map(ProfileId)

  override def createRoute(basePathMatcher: PathMatcher[Unit]): Route = {
    path(basePathMatcher / "profile" / MatchProfileId) { profileId =>
      requestTrace("getProfile") { implicit traceSpan: TraceSpan =>
        get {
          logger.debug(Map("profileId" -> profileId.asString))
          val profile = Profile(profileId, "Kendra Elford", "kendra@enkode.me")
          complete(ProfileResponse(profile))
        }
      }
    } ~ {
      path(basePathMatcher / "login") {
        (post & entity(as[LoginRequest])) { loginRequest =>
          requestTrace("login") { implicit traceSpan =>
            complete {
              profileService.login(loginRequest.login, loginRequest.password).map(login => {
                login.map(_.user.profile.fullName).fold(_.toString, identity)
              })
            }
          }
        }
      }
    }
  }
}
