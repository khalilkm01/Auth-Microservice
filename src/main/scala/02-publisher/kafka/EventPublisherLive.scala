package publisher.kafka

import models.infrastructure.KafkaMessage
import publisher.EventPublisher
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.{Task, ZIO, ZLayer}

case class EventPublisherLive(producer: Producer) extends EventPublisher {
  import KafkaMessage.*
  override def publishEvent(
      kafkaMessage: KafkaMessage,
      topic: String
  ): Task[Either[String, Unit]] =
    producer
      .produce(
        topic,
        kafkaMessage.key,
        kafkaMessage,
        Serde.string,
        kafkaMessageSerde
      )
      .foldZIO(
        failure ⇒ ZIO.left(failure.getMessage),
        _ ⇒ ZIO.right(())
      )
}

object EventPublisherLive:
  lazy val layer: ZLayer[Producer, Nothing, EventPublisher] =
    ZLayer.fromFunction(EventPublisherLive(_))
