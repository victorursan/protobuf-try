package com.victor.proto

import akka.http.scaladsl.marshalling.{ PredefinedToEntityMarshallers, ToEntityMarshaller }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import scalapb.{ GeneratedMessage, GeneratedMessageCompanion, Message }

trait ProtobufSupport {
  implicit def protobufMarshaller[T <: GeneratedMessage]: ToEntityMarshaller[T] =
    PredefinedToEntityMarshallers.ByteArrayMarshaller.compose[T](_.toByteArray)
  implicit def protobufUnmarshaller[T <: GeneratedMessage with Message[T]](implicit companion: GeneratedMessageCompanion[T]): FromEntityUnmarshaller[T] =
    Unmarshaller.byteArrayUnmarshaller.map[T](companion.parseFrom)
}
