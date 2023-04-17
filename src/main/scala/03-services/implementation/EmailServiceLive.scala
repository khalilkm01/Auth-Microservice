package services.implementation

import models.common.ServerError
import models.persisted.Email
import services.EmailService
import org.joda.time.DateTime
import repositories.quill.QuillContext
import repositories.EmailRepository
import zio.*

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class EmailServiceLive(emailRepository: EmailRepository) extends EmailService with ServiceAssistant:

  import EmailService._
  import QuillContext._

  private val dataSource: ULayer[DataSource] = dataSourceLayer

  override def findByEmailAddress(findByEmailAddressDTO: FindByEmailAddressDTO): IO[ServerError, Email] = transaction {
    emailRepository
      .getByEmailAddress(findByEmailAddressDTO.emailAddress, findByEmailAddressDTO.user)
  }
    .provide(dataSource)
    .catchAll(handleError)

  override def createEmail(
    createEmailDTO: CreateEmailDTO
  ): IO[ServerError, Email] =
    transaction {
      for {
        createdAt <- ZIO.succeed(DateTime.now)
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
                user = createEmailDTO.user,
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
      } yield email
    }.provide(dataSource).catchAll(handleError)

  override def updateEmailAddress(
    updateEmailAddressDTO: UpdateEmailAddressDTO
  ): IO[ServerError, Email] = transaction {
    for {
      dateTime      <- ZIO.succeed(DateTime.now)
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
  }.provide(dataSource).catchAll(handleError)

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
