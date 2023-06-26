package services

import models.common.{ AuthData, ServerError }
import models.enums.{ CountryCode, UserType }
import models.persisted.Login
import zio.{ IO, Task }

import java.util.UUID

trait LoginService:
  import LoginService._
  def createLogin(createLoginDTO: CreateLoginDTO): IO[ServerError, AuthData]
  def updateLoginPassword(
    updateLoginPasswordDTO: UpdateLoginPasswordDTO
  ): IO[ServerError, Boolean]
  def loginByEmail(
    loginByEmailDTO: LoginByEmailDTO
  ): IO[ServerError, AuthData]

object LoginService:
  case class CreateLoginDTO(
    user: UserType,
    emailAddress: String,
    digits: String,
    countryCode: CountryCode,
    code: String,
    password: String
  )
  case class UpdateLoginPasswordDTO(
    loginId: UUID,
    newPassword: String,
    currentPassword: String
  )
  case class LoginByEmailDTO(emailAddress: String, password: String, user: UserType)
