package services.implementation

import models.common.ServerError
import models.persisted.Email
import models.infrastructure.*
import models.infrastructure.Event.given
import publisher.EventPublisher
import repositories.quill.QuillContext
import repositories.EmailRepository
import services.EmailService

import zio.*
import zio.json._
import org.joda.time.DateTime

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class EmailServiceLive(emailRepository: EmailRepository, eventPublisher: EventPublisher)
    extends EmailService
    with ServiceAssistant:

  import EmailService._
  import QuillContext._

  override def findByEmailAddress(findByEmailAddressDTO: FindByEmailAddressDTO): IO[ServerError, Email] = transact {
    emailRepository
      .getByEmailAddress(findByEmailAddressDTO.emailAddress, findByEmailAddressDTO.user)
  }

  override def createEmail(
    createEmailDTO: CreateEmailDTO
  ): IO[ServerError, UUID] =
    transact {
      for
        createdAt <- ZIO.succeed(DateTime.now())
        existingEmail <- emailRepository.checkExistingEmailAddress(
          emailAddress = createEmailDTO.emailAddress,
          user = createEmailDTO.user
        )
        email <-
          if (!existingEmail)
            emailRepository.create(
              Email(
                id = UUID.randomUUID,
                emailAddress = createEmailDTO.emailAddress,
                verified = false,
                connected = false,
                userType = createEmailDTO.user,
                createdAt = createdAt,
                updatedAt = createdAt
              )
            )
          else
            ZIO.fail(
              ServerError.ServiceError(
                ServerError.ServiceErrorMessage.IllegalServiceCall
              )
            )
        _ <- eventPublisher.publishEvent(
          email.id,
          EmailCreatedEvent(email.id, createEmailDTO.user)
        )
      yield email.id
    }

  override def connectEmail(connectEmailDTO: ConnectEmailDTO): IO[ServerError, Boolean] = transact {
    for
      connected <- emailRepository.connectEmail(id = connectEmailDTO.emailId)
      _ <- eventPublisher.publishEvent(
        connectEmailDTO.emailId,
        EmailConnectedEvent(
          emailId = connectEmailDTO.emailId,
          user = connectEmailDTO.user
        )
      )
    yield connected
  }
  override def disconnectEmail(disconnectEmailDTO: DisconnectEmailDTO): IO[ServerError, Boolean] = transact {
    for
      disconnected <- emailRepository.disconnectEmail(id = disconnectEmailDTO.id)

      _ <- eventPublisher.publishEvent(
        KafkaMessage(
          disconnectEmailDTO.id,
          EmailDisconnectedEvent(emailId = disconnectEmailDTO.id)
        ),
        EventTopics.EMAIL_TOPIC
      )
    yield disconnected
  }

  override def updateEmailAddress(
    updateEmailAddressDTO: UpdateEmailAddressDTO
  ): IO[ServerError, Email] = transact {
    for {
      dateTime      <- ZIO.succeed(DateTime.now())
      existingEmail <- emailRepository.checkExistingId(updateEmailAddressDTO.id)
      oldEmail <-
        if (existingEmail)
          emailRepository.getById(updateEmailAddressDTO.id)
        else
          ZIO.fail(
            ServerError.ServiceError(
              ServerError.ServiceErrorMessage.IdNotFound(
                updateEmailAddressDTO.id
              )
            )
          )
      email <- emailRepository.save(
        Email(
          id = oldEmail.id,
          emailAddress = updateEmailAddressDTO.emailAddress,
          verified = false,
          userType = updateEmailAddressDTO.user,
          connected = oldEmail.connected,
          createdAt = oldEmail.createdAt,
          updatedAt = dateTime
        )
      )
      _ <- eventPublisher.publishEvent(
        email.id,
        EmailUpdatedEvent(emailId = email.id, emailAddress = email.emailAddress)
      )
    } yield email
  }

  override def isEmailUsable(
    isEmailUsableDTO: IsEmailUsableDTO
  ): Task[Boolean] = transact {
    emailRepository
      .checkExistingEmailAddress(emailAddress = isEmailUsableDTO.emailAddress, user = isEmailUsableDTO.user)
  }

  override def deleteEmail(deleteEmailDTO: DeleteEmailDTO): IO[ServerError, Unit] =
    transact {
      emailRepository.delete(List(deleteEmailDTO.id)).unit
    }

object EmailServiceLive:
  lazy val layer: ZLayer[EmailRepository with EventPublisher, Nothing, EmailService] =
    ZLayer.fromFunction(EmailServiceLive(_, _))
