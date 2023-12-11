package publisher

import models.infrastructure.KafkaMessage
import publisher.kafka.EventPublisherLive
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID

object EventPublisherSpec extends ZIOSpecDefault:

  private val layer: TaskLayer[EventPublisher] = KafkaMock.producerLayer >+> EventPublisherLive.layer

  private val kafkaMessage: KafkaMessage = KafkaMessage(UUID.randomUUID, "value")
  private val defaultTopic: String       = "defaultTopic"
  def spec: Spec[Any, Throwable] = suite("EventPublisherSpec") {
    test("Publisher publishes event and returns unit correctly") {
      for
        publisher <- ZIO.service[EventPublisher]
        event     <- publisher.publishEvent(kafkaMessage, defaultTopic)
        _         <- ZIO.logInfo(s"Finished ${event}")
      yield assertCompletes
    }.provideSomeLayer(EventPublisherLive.layer)
  }.provideShared(KafkaMock.producerLayer)
