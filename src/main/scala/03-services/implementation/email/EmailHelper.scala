package services.implementation.email

import models.common.ServerError
import models.enums.UserType
import models.persisted.Email
import repositories.EmailRepository
import services.EmailService
import zio.{ RIO, ZIO }
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource
trait EmailHelper:
  protected def createEmailHelper(
    emailRepository: EmailRepository,
    emailAddress: String,
    user: UserType
  ): RIO[DataSource, UUID] = for {
    createdAt <- ZIO.succeed(DateTime.now())
    existingEmail <- emailRepository.checkExistingEmailAddress(
      emailAddress = emailAddress,
      user = user
    )
    email <-
      if (!existingEmail)
        emailRepository.create(
          Email(
            id = UUID.randomUUID,
            emailAddress = emailAddress,
            verified = false,
            connected = false,
            user = user,
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
  } yield email.id
