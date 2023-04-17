package models.common

import zio.json.*
import zio.json.DeriveJsonCodec.*
import zio.json.DeriveJsonEncoder.*
import zio.json.DeriveJsonDecoder.*
import zio.json.JsonCodec.*
import zio.json.JsonEncoder.*
import zio.json.JsonDecoder.*
import scala.deriving.Mirror

object JsonHelper:

  // generate json encoders and decoders from the zio library for all classes
  // with the jsonField annotation

  inline def deriveCodec[A](using m: Mirror.Of[A]): JsonCodec[A] = DeriveJsonCodec.gen[A]
