package gateway.infrastructure

import gateway.Gateway
import models.infrastructure.KafkaMessage
import zio.json.*
import zio.kafka.consumer.*
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{ RIO, RLayer, Task, ZIO, ZLayer }

final case class KafkaGateway(consumer: Consumer, eventHandler: EventHandler, topics: Subscription)
    extends KafkaGateway.GatewayOut:
  import KafkaMessage.{ kafkaMessageSerde, given }

  override val startingMessage: String = "Started consuming events..."
  override def start: RIO[Services, Unit] = {

    ZIO.logInfo("Started consuming events...") *> {

      val eventStream: ZStream[Any, Throwable, CommittableRecord[String, KafkaMessage]] =
        consumer
          .plainStream(topics, Serde.string, kafkaMessageSerde)

      val processStream: ZStream[Any, Throwable, Offset] =
        eventStream
          .map { event ⇒ (event.record.value, event.offset) }
          .tap { case (event, _) ⇒ eventHandler.handleEvent(event) }
          .map(_._2)

      val commitOffset: ZStream[Any, Throwable, Unit] =
        processStream.aggregateAsync(Consumer.offsetBatches).mapZIO(_.commit)

      commitOffset.runDrain
    }
  }

object KafkaGateway:
  type Services              = Any
  private type EnvironmentIn = Services with Consumer with EventHandler with Subscription
  private type GatewayOut = Gateway[
    EnvironmentIn
  ]
  lazy val layer: RLayer[EnvironmentIn, KafkaGateway] = ZLayer.scoped(
    for
      topics       <- ZIO.service[Subscription]
      consumer     <- ZIO.service[Consumer]
      eventHandler <- ZIO.service[EventHandler]
    yield new KafkaGateway(consumer = consumer, eventHandler = eventHandler, topics = topics)
  )
