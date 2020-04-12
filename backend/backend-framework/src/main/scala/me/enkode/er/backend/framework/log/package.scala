package me.enkode.er.backend.framework

package object log {
  type LoggingShow[T] = T => String

  implicit val idLoggingShow: LoggingShow[String] = identity

  implicit def mapLoggingShow[K: LoggingShow, V: LoggingShow]: LoggingShow[Map[K, V]] = { map: Map[K, V] =>
    val kShow = implicitly[LoggingShow[K]]
    val vShow = implicitly[LoggingShow[V]]
    map.map({case (k, v) => s"${kShow(k)}=${vShow(v)}"}).mkString(", ")
  }

}
