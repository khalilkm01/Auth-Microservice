package services

import models.enums.CountryCode
import models.enums.UserType
import models.common.ServerError
import models.persisted.ContactNumber
import zio.{ IO, Task }

import java.util.UUID

trait ContactNumberService:
  import ContactNumberService._

  def checkExistingNumber(checkExistingNumberDTO: CheckExistingNumberDTO): IO[ServerError, Boolean]
  def requestPhoneCode(requestPhoneCodeDTO: RequestPhoneCodeDTO): IO[ServerError, Boolean]
  def findNumber(findNumberDTO: FindNumberDTO): IO[ServerError, ContactNumber]
  def createContactNumber(
    createContactNumberDTO: CreateContactNumberDTO
  ): IO[ServerError, ContactNumber]
  def verifyPhoneCode(verifyPhoneCodeDTO: VerifyPhoneCodeDTO): IO[ServerError, Boolean]

  def connectNumber(connectNumberDTO: ConnectNumberDTO): IO[ServerError, Boolean]
  def disconnectNumber(disconnectNumberDTO: DisconnectNumberDTO): IO[ServerError, Boolean]
//  def deleteContactNumber(
//      deleteContactNumberDTO: DeleteContactNumberDTO
//  ): Task[ContactNumber]

object ContactNumberService:
  case class CheckExistingNumberDTO(countryCode: CountryCode, digits: String, user: UserType)
  case class FindNumberDTO(countryCode: CountryCode, digits: String, user: UserType)
  //  case class DeleteContactNumberDTO()
  case class CreateContactNumberDTO(
    countryCode: CountryCode,
    digits: String,
    user: UserType
  )
  case class RequestPhoneCodeDTO(
    countryCode: CountryCode,
    digits: String
  )
  case class VerifyPhoneCodeDTO(id: UUID, code: String, user: UserType)

  case class ConnectNumberDTO(id: UUID, code: String, user: UserType)
  case class DisconnectNumberDTO(id: UUID)
