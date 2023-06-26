package services.implementation

import models.common.ServerError
import models.persisted.Email
import services.EmailService
import org.joda.time.DateTime
import repositories.quill.QuillContext
import repositories.EmailRepository
import services.implementation.email.EmailHelper
import zio.*

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class EmailServiceLive(emailRepository: EmailRepository) extends EmailService with EmailHelper with ServiceAssistant:

  import EmailService._
  import QuillContext._

  private val dataSource: ULayer[DataSource] = dataSourceLayer

  override def findByEmailAddress(findByEmailAddressDTO: FindByEmailAddressDTO): IO[ServerError, Email] = transact {
    emailRepository
      .getByEmailAddress(findByEmailAddressDTO.emailAddress, findByEmailAddressDTO.user)
  }

  override def createEmail(
    createEmailDTO: CreateEmailDTO
  ): IO[ServerError, UUID] =
    transact {
      createEmailHelper(
        emailAddress = createEmailDTO.emailAddress,
        user = createEmailDTO.user,
        emailRepository = emailRepository
      )
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
          user = updateEmailAddressDTO.user,
          connected = oldEmail.connected,
          createdAt = oldEmail.createdAt,
          updatedAt = dateTime
        )
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
  lazy val layer: ZLayer[EmailRepository, Nothing, EmailService] =
    ZLayer.fromFunction(EmailServiceLive(_))
