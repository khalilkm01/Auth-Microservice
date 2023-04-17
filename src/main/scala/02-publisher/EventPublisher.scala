package publisher

import models.infrastructure.KafkaMessage
import zio.Task


trait EventPublisher:
  def publishEvent(
      kafkaMessage: KafkaMessage,
      topic: String
  ): Task[Either[String, Unit]]
