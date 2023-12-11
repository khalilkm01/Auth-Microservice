package publisher.kafka

import models.infrastructure.{ Event, KafkaMessage }
import models.infrastructure.Event.given
import publisher.EventPublisher

import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.{ Task, ZIO, ZLayer }
import zio.json._

import java.util.UUID

case class EventPublisherLive(producer: Producer) extends EventPublisher:
  import KafkaMessage.*
  override def publishEvent(
    key: UUID,
    event: Event
  ): Task[Either[String, Unit]] =
    ZIO.logInfo(s"Publishing Event with key: $key to topic: ${event.getTopic}") *> producer
      .produce(
        topic = event.getTopic.toString,
        key = key,
        value = KafkaMessage(key = key, payload = event.toJson, channel = event.getChannel),
        keySerializer = Serde.uuid,
        valueSerializer = kafkaMessageSerde
      )
      .foldZIO(
        failure ⇒ ZIO.left(failure.getMessage),
        _ ⇒ ZIO.right(())
      )

object EventPublisherLive:
  lazy val layer: ZLayer[Producer, Nothing, EventPublisher] =
    ZLayer.fromFunction(EventPublisherLive(_))
