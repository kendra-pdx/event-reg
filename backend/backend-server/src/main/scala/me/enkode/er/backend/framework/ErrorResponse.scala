package me.enkode.er.backend.framework

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import me.enkode.er.backend.server.CORSSupport
import ujson._

import scala.util.control.NonFatal

object ErrorResponse extends Directives with μPickleMarshallingSupport with CORSSupport {
  case class ClientError(message: String) extends RuntimeException
  case class ServerError(message: String) extends RuntimeException



  implicit val akkaErrorHandler: ExceptionHandler = {
    ExceptionHandler {
      case ClientError(message) =>
        val json = Obj(
          "message" -> Str(message)
        )
        withCORS(complete((StatusCodes.BadRequest, json)))

      case ServerError(message) =>
        val json = Obj(
          "message" -> Str(message)
        )
        withCORS(complete((StatusCodes.InternalServerError, json)))

      case NonFatal(t) =>
        val json = Obj(
          "message" -> Str(t.getMessage)
        )
        withCORS(complete((StatusCodes.InternalServerError, json)))
    }
  }
}
