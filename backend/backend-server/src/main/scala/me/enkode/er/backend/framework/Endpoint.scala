package me.enkode.er.backend.framework

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import me.enkode.er.backend.framework.log.TraceSpan

trait Endpoint {
  def name: String
  def createRoute(basePathMatcher: PathMatcher[Unit]): Route

  def requestTrace(childSpanName: String) = {
    (optionalHeaderValueByName("Trace-Id") &
    optionalHeaderValueByName("Span-Id") &
    optionalHeaderValueByName("Span-Name")).tmap({
      case (Some(traceId), Some(spanId), Some(spanName)) =>
        TraceSpan(traceId, spanId, spanName, None).child(childSpanName)
      case _ =>
        TraceSpan(childSpanName)
    })
  }
}
