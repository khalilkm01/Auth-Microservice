package infrastructure

import models.infrastructure.Event

import io.getquill.context.ZioJdbc.QIO
import models.infrastructure.KafkaMessage
import zio.Task
import zio.json.JsonDecoder

trait EventHandler:
  def handleEvent(
    event: KafkaMessage
  ): Task[Either[String, Unit]]

  def parseJsonPayload(payload: String)(implicit
    jsonDecoder: JsonDecoder[Event]
  ): Either[String, Event]

  def processResponse[R](result: Task[R]): Task[Either[String, Unit]]
