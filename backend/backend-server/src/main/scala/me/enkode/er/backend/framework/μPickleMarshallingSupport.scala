package me.enkode.er.backend.framework

import akka.http.scaladsl.model.MediaTypes
import ujson.Value
import upickle.default._

trait μPickleMarshallingSupport {

  import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
  import akka.http.scaladsl.model.MediaTypes.`application/json`
  import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}

  import scala.language.implicitConversions

  implicit def μPickleUnmarshallerConverter[T](reader: Reader[T]): FromEntityUnmarshaller[T] =
    μPickleUnmarshaller(reader)

  implicit def μPickleUnmarshaller[T](implicit reader: Reader[T]): FromEntityUnmarshaller[T] =
    μPickleValueUnmarshaller.map(value => read[T](value))

  implicit def μPickleValueUnmarshaller: FromEntityUnmarshaller[Value.Value] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(`application/json`).mapWithCharset { (data, charset) =>
      val i = data.decodeString(charset.nioCharset.name)
      ujson.read(i)
    }

  private def compactWrite[T: Writer](value: T) = write[T](
    value, indent = -1, escapeUnicode = false
  )

  implicit def μPickleMarshallerConverter[T](writer: Writer[T]): ToEntityMarshaller[T] = {
    μPickleMarshaller[T](writer)
  }


  implicit def μPickleMarshaller[T](implicit writer: Writer[T]): ToEntityMarshaller[T] = {
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(compactWrite[T])
  }

  implicit def μPickleValueMarshaller(): ToEntityMarshaller[Value] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(compactWrite[Value])
}
