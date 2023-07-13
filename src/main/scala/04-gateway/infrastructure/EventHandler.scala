package gateway.infrastructure

import models.infrastructure.{ Event, KafkaMessage }

import zio.Task
import zio.json.JsonDecoder
import zio.kafka.consumer.Subscription

trait EventHandler:
  def handleEvent(
    event: KafkaMessage
  ): Task[Either[String, Unit]]

  def parseJsonPayload(payload: String)(implicit
    jsonDecoder: JsonDecoder[Event]
  ): Either[String, Event]

  def processResponse[R](result: Task[R]): Task[Either[String, Unit]]
