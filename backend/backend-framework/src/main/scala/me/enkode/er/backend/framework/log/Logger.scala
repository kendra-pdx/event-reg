package me.enkode.er.backend.framework.log

import java.time.Instant


trait Logger {
  def debug[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit

  def info[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit

  def error[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit
}

object ConsoleLogger {
  sealed trait Level {
    def >= (other: Level): Boolean = this.priority >= other.priority
    val priority: Int
  }
  object Level {
    object Debug extends Level {
      override val priority = 3
    }

    object Info extends Level {
      override val priority = 2
    }

    object Error extends Level {
      override val priority = 1
    }
  }
}
class ConsoleLogger(name: String, level: ConsoleLogger.Level) extends Logger {

  import me.enkode.er.backend.framework.log.ConsoleLogger._

  private def traceString(traceSpan: TraceSpan): String = {
    (Map(
      "traceId" -> traceSpan.traceId,
      "spanId" -> traceSpan.spanId,
      "spanName" -> traceSpan.spanName
    ) ++ (traceSpan.parentSpanId match {
      case Some(parentSpanId) => Map("parentSpanId" -> parentSpanId)
      case None => Map.empty[String, String]
    })).map({ case (k, v) => s"$k=$v" })
      .mkString("trace(", ", ", ")")
  }

  private def head[TSC: TraceSpan.Container](levelStr: String)(implicit traceSpanContainer: TSC): String = {
    val traceContainer = implicitly[TraceSpan.Container[TSC]]
    val traceSpan = traceContainer.trace(traceSpanContainer)
    s"$levelStr ($name) @${Instant.now().toString} ${traceString(traceSpan)}"
  }

  override def debug[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit = {
    if (level >= Level.Debug) {
      val show = implicitly[LoggingShow[T]]
      println(s"${head("DEBUG")}: ${show(message)}")
    }
  }

  override def info[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit = {
    if (level >= Level.Info) {
      val show = implicitly[LoggingShow[T]]
      println(s"${head("INFO")}: ${show(message)}")
    }
  }

  override def error[T: LoggingShow, TSC: TraceSpan.Container](message: => T)(implicit traceSpanContainer: TSC): Unit = {
    if (level >= Level.Error) {
      val show = implicitly[LoggingShow[T]]
      println(s"${head("ERROR")}: ${show(message)}")
    }
  }
}