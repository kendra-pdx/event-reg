package me.enkode.er.backend.framework

import me.enkode.er.backend.framework.auth.AuthInfo
import me.enkode.er.backend.framework.log.TraceSpan

trait EndpointContext {
  def authToken: AuthInfo
  def traceSpan: TraceSpan
}
