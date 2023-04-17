package infrastructure.kafka

import infrastructure.{ EventBus, EventHandler }
import models.infrastructure.KafkaMessage
import zio.json.*
import zio.kafka.consumer.*
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{ Task, ZIO, ZLayer }

case class EventBusLive(
  consumer: Consumer,
  eventHandler: EventHandler
) extends EventBus:
  import KafkaMessage.*

  override def start(
    parSize: Int,
    topics: Subscription
  ): Task[Unit] = {

    ZIO.log("Started consuming events...") *> {

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

object EventBusLive:
  lazy val layer: ZLayer[
    Consumer with EventHandler,
    Throwable,
    EventBus
  ] =
    ZLayer.scoped(
      for {
        consumer     <- ZIO.service[Consumer]
        eventHandler <- ZIO.service[EventHandler]
      } yield new EventBusLive(consumer, eventHandler)
    )
