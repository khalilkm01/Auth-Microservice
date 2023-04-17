package services

import models.common.{ AuthData, ServerError }
import models.enums.UserType
import models.persisted.Login
import zio.{ IO, Task }

import java.util.UUID

trait LoginService:
  import LoginService._
  def createLogin(createLoginDTO: CreateLoginDTO): Task[Login]
  def updateLoginPassword(
    updateLoginPasswordDTO: UpdateLoginPasswordDTO
  ): Task[Login]
  def loginByEmail(
    loginByEmailDTO: LoginByEmailDTO
  ): IO[ServerError, AuthData]

object LoginService:
  case class CreateLoginDTO(
    user: UserType,
    contactNumberId: UUID,
    emailId: UUID,
    password: String
  )
  case class UpdateLoginPasswordDTO(
    loginId: UUID,
    newPassword: String,
    currentPassword: String
  )
  case class LoginByEmailDTO(email: String, password: String, user: UserType)
