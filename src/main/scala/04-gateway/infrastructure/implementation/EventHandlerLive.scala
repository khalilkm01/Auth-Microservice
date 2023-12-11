package gateway.infrastructure.implementation

import models.infrastructure.*
import models.infrastructure.Event.given
import models.enums.CountryCode
import publisher.EventPublisher
import services.{ ContactNumberService, EmailService, LoginService }
import gateway.infrastructure.EventHandler

import zio.json.JsonDecoder
import zio.{ Task, TaskLayer, ULayer, ZIO, ZLayer }

import javax.sql.DataSource

final case class EventHandlerLive(
  publisher: EventPublisher,
  loginService: LoginService,
  emailService: EmailService,
  contactNumberService: ContactNumberService
) extends EventHandler:

  override def handleEvent(
    event: KafkaMessage
  ): Task[Either[String, Unit]] =
    parseJsonPayload(event.payload) match
      case Left(decodeError) ⇒ ZIO.left(decodeError)
      case Right(event) ⇒
        processResponse {
          event match
            case LoginCreatedEvent(loginId, contactNumberId, emailId, code, user) ⇒
              emailService
                .connectEmail(
                  EmailService.ConnectEmailDTO(emailId = emailId, loginId = loginId, user = user)
                )
              <&>
              contactNumberService.connectNumber(
                ContactNumberService.ConnectNumberDTO(
                  contactNumberId = contactNumberId,
                  loginId = loginId,
                  user = user,
                  code = code
                )
              )

            case _ ⇒
              ZIO.right(())
        }

  override def parseJsonPayload(payload: String)(using
    jsonDecoder: JsonDecoder[Event]
  ): Either[String, Event] = jsonDecoder.decodeJson(payload)

  override def processResponse[R](
    result: Task[R]
  ): Task[Either[String, Unit]] =
    result.fold(failure ⇒ Left(failure.getMessage), _ ⇒ Right(()))

object EventHandlerLive:
  lazy val layer
    : ZLayer[LoginService with EmailService with ContactNumberService with EventPublisher, Nothing, EventHandler] =
    ZLayer.fromFunction(EventHandlerLive(_, _, _, _))
//    ZLayer.fromFunction(EventHandlerLive())
