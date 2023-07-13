package gateway.infrastructure.implementation

import models.infrastructure.{ Event, KafkaMessage }
import gateway.infrastructure.EventHandler
import io.getquill.context.ZioJdbc.QIO
import io.getquill.jdbczio.Quill
import zio.json.JsonDecoder
import zio.{ Task, TaskLayer, ULayer, ZIO, ZLayer }

import javax.sql.DataSource

final case class EventHandlerLive(
) extends EventHandler:
  import Event.*

  override def handleEvent(
    event: KafkaMessage
  ): Task[Either[String, Unit]] = {
    parseJsonPayload(event.payload) match
      case Left(decodeError) ⇒ ZIO.left(decodeError)
      case Right(event) ⇒
        event.match

          case _ ⇒
            ZIO.fail(throw new Error("No Event Matched"))
  }

  override def parseJsonPayload(payload: String)(implicit
    jsonDecoder: JsonDecoder[Event]
  ): Either[String, Event] = jsonDecoder.decodeJson(payload)

  override def processResponse[R](
    result: Task[R]
  ): Task[Either[String, Unit]] =
    result.fold(failure ⇒ Left(failure.getMessage), _ ⇒ Right(()))

object EventHandlerLive:

  lazy val layer: TaskLayer[EventHandler] =
    ZLayer(ZIO.attempt(new EventHandlerLive))
//    ZLayer.fromFunction(EventHandlerLive())
