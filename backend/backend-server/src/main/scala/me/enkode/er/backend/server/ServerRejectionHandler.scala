package me.enkode.er.backend.server

import akka.http.scaladsl.server.RejectionHandler
import me.enkode.er.backend.framework.CORSSupport

object ServerRejectionHandler extends CORSSupport {
  implicit val rejectionHandler = RejectionHandler.default.mapRejectionResponse(_.withHeaders(corsResponseHeaders))
}
