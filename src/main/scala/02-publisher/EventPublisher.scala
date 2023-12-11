package publisher

import models.infrastructure.{ Event, KafkaMessage }
import zio.Task

import java.util.UUID

trait EventPublisher:
  def publishEvent(
                    key: UUID,
                    event: Event
                  ): Task[Either[String, Unit]]
