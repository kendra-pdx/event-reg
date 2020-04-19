package me.enkode.er.backend.auth

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.data.Validated._
import me.enkode.er.backend.auth.AuthInfo.{Scope, Subject}
import me.enkode.er.backend.framework.log.{Logger, TraceSpan}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AuthDirectives {
  type Require = AuthInfo => Boolean

  def hasAllScopes(scopes: Scope*): Require = { authInfo =>
    scopes.forall(authInfo.scopes.contains(_))
  }

  def subjectIs(subject: Subject): Require = { authInfo =>
    authInfo.subject == subject
  }
}

trait AuthDirectives {

  import AuthDirectives._

  def authService: AuthService[Future]

  def logger: Logger


  private def findAuthorization(ctx: RequestContext): Option[HttpHeader] = {
    ctx.request.headers.find(_.is("authorization"))
  }

  def requireAuth(requires: Require*)(implicit traceSpan: TraceSpan): Directive1[AuthInfo] = {
    extract[Option[HttpHeader]](findAuthorization).flatMap({
      case Some(header) =>
        val authToken = authService.parseJwt(header.value().replaceFirst("Bearer ", ""))
        onComplete(authService.validateAuthToken(authToken)).flatMap({
          case Success(Valid(authInfo)) =>
            if (requires.forall(_ (authInfo))) {
              provide(authInfo)
            } else {
              logger.info(s"auth failure: requirements")
              reject(AuthorizationFailedRejection)
            }

          case Success(Invalid(errors)) =>
            logger.info(s"auth failure: ${errors.map(_.toString).toList.mkString(", ")}")
            reject(AuthorizationFailedRejection)

          case Failure(exception) =>
            logger.error(s"auth failure: ${exception.getMessage}")
            reject(AuthorizationFailedRejection)
        })

      case None =>
        logger.info(s"auth failure: no auth")
        reject(AuthorizationFailedRejection)
    })
  }
}
