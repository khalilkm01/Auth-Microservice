package models.infrastructure

import zio.ZIO
import zio.json.*
import zio.kafka.serde.Serde

case class KafkaMessage(key: String, payload: String)

object KafkaMessage:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[KafkaMessage] = deriveCodec

  val kafkaMessageSerde: Serde[Any, KafkaMessage] = Serde.string.inmapM { string =>
    ZIO.fromEither(
      string
        .fromJson[KafkaMessage]
        .left
        .map(errorMessage => new RuntimeException(errorMessage))
    )
  } { event =>
    ZIO.attempt(event.toJson)
  }
