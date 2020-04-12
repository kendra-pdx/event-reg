package me.enkode.er.backend.framework.log

import scala.util.Random

object TraceSpan {
  implicit val idTraceSpanContainer: TraceSpan.Container[TraceSpan] = Container[TraceSpan](identity)

  def genId(prefix: String): String = {
    prefix + ":" + Random.alphanumeric.take(8).mkString
  }

  def child(parent: TraceSpan, childSpanName: String): TraceSpan = new TraceSpan {
    override val traceId: String = parent.traceId

    override val spanId: String = genId("s")

    override val parentSpanId: Option[String] = Some(parent.spanId)

    override val spanName: String = childSpanName

    override def child(spanName: String): TraceSpan = TraceSpan.child(this, spanName)
  }

  def apply(newSpanName: String): TraceSpan = apply(genId("t"), genId("s"), newSpanName, None)

  def apply(_traceId: String, _spanId: String, _spanName: String, _parentSpanId: Option[String]): TraceSpan = new TraceSpan {
    override def traceId: String = _traceId

    override def spanId: String = _spanId

    override def spanName: String = _spanName

    override def parentSpanId: Option[String] = _parentSpanId

    override def child(childSpanName: String): TraceSpan = TraceSpan.child(this, childSpanName)
  }

  trait Container[T] {
    def trace(obj: T): TraceSpan
  }

  object Container {
    def apply[T](getTraceSpan: (T) => TraceSpan): Container[T] = (obj: T) => getTraceSpan(obj)
  }
}

trait TraceSpan {
  def traceId: String

  def spanId: String

  def spanName: String

  def parentSpanId: Option[String]

  def child(childSpanName: String): TraceSpan
}
