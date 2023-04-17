package services

import models.common.ServerError
import models.enums.UserType
import models.persisted.Email
import zio.{ IO, Task }

import java.util.UUID

trait EmailService:
  import EmailService._

  def findByEmailAddress(findByEmailAddressDTO: FindByEmailAddressDTO): IO[ServerError, Email]

  def createEmail(createEmailDTO: CreateEmailDTO): IO[ServerError, Email]

  def updateEmailAddress(
    updateEmailAddressDTO: UpdateEmailAddressDTO
  ): IO[ServerError, Email]

  def isEmailUsable(isEmailUsableDTO: IsEmailUsableDTO): Task[Boolean]

  def deleteEmail(deleteEmailDTO: DeleteEmailDTO): IO[ServerError, Unit]

object EmailService:
  case class FindByEmailAddressDTO(emailAddress: String, user: UserType)
  case class CreateEmailDTO(emailAddress: String, user: UserType)
  case class UpdateEmailAddressDTO(id: UUID, emailAddress: String, user: UserType)
  case class IsEmailUsableDTO(emailAddress: String, user: UserType)

  case class DeleteEmailDTO(id: UUID)
