package models.infrastructure

import zio.ZIO
import zio.json.*
import zio.kafka.serde.Serde

import java.util.UUID

case class KafkaMessage(key: UUID, payload: String, channel: Channel)

object KafkaMessage:
  import models.common.JsonHelper.deriveCodec
  import models.infrastructure.Channel.given

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
