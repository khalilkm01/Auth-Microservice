package services.implementation

import models.enums.CountryCode
import models.common.ServerError
import models.persisted.ContactNumber
import clients.TwilioClient
import repositories.ContactNumberRepository
import services.ContactNumberService
import repositories.quill.QuillContext
import zio.*
import org.joda.time.DateTime
import services.implementation.contactNumber.ContactNumberHelper

import java.util.UUID
import javax.sql.DataSource

class ContactNumberServiceLive(
  twilioClient: TwilioClient,
  contactNumberRepository: ContactNumberRepository
) extends ContactNumberService
    with ContactNumberHelper
    with ServiceAssistant:

  import ContactNumberService._
  import QuillContext._

  private val dataSource: ULayer[DataSource] = dataSourceLayer

  override def checkExistingNumber(checkExistingNumberDTO: CheckExistingNumberDTO): IO[ServerError, Boolean] =
    transact(
      contactNumberRepository.checkExistingNumber(
        countryCode = checkExistingNumberDTO.countryCode,
        digits = checkExistingNumberDTO.digits,
        user = checkExistingNumberDTO.user
      )
    )

  override def requestPhoneCode(requestPhoneCodeDTO: RequestPhoneCodeDTO): IO[ServerError, Boolean] =
    twilioClient
      .requestPhoneCode(
        countryCode = requestPhoneCodeDTO.countryCode,
        digits = requestPhoneCodeDTO.digits
      )
      .catchAll(handleError)

  override def findNumber(findNumberDTO: FindNumberDTO): IO[ServerError, ContactNumber] =
    transact {
      contactNumberRepository
        .getByNumber(
          countryCode = findNumberDTO.countryCode,
          digits = findNumberDTO.digits,
          user = findNumberDTO.user
        )
    }

  override def createContactNumber(
    createContactNumberDTO: CreateContactNumberDTO
  ): IO[ServerError, ContactNumber] = transact {
    for {
      createdAt <- ZIO.succeed(DateTime.now())
      existing <- contactNumberRepository.checkExistingNumber(
        countryCode = createContactNumberDTO.countryCode,
        digits = createContactNumberDTO.digits,
        user = createContactNumberDTO.user
      )
      codeRequested <- twilioClient.requestPhoneCode(
        countryCode = createContactNumberDTO.countryCode,
        digits = createContactNumberDTO.digits
      )
      contactNumber <-
        if (!existing && codeRequested)
          contactNumberRepository.create(
            ContactNumber(
              id = UUID.randomUUID,
              countryCode = createContactNumberDTO.countryCode,
              digits = createContactNumberDTO.digits,
              connected = false,
              user = createContactNumberDTO.user,
              createdAt,
              createdAt
            )
          )
        else
          ZIO.fail(
            ServerError.ServiceError(
              ServerError.ServiceErrorMessage.IllegalServiceCall
            )
          )
    } yield contactNumber
  }

  override def verifyPhoneCode(
    verifyPhoneCodeDTO: VerifyPhoneCodeDTO
  ): IO[ServerError, Boolean] = transact {
    verifyPhoneCodeHelper(
      id = verifyPhoneCodeDTO.id,
      code = verifyPhoneCodeDTO.code,
      user = verifyPhoneCodeDTO.user,
      twilioClient = twilioClient,
      contactNumberRepository = contactNumberRepository
    )
  }

  override def connectNumber(
    connectNumberDTO: ConnectNumberDTO
  ): IO[ServerError, Boolean] = transact {
    connectNumberHelper(
      id = connectNumberDTO.id,
      code = connectNumberDTO.code,
      user = connectNumberDTO.user,
      contactNumberRepository = contactNumberRepository,
      twilioClient = twilioClient
    )
  }
  override def disconnectNumber(
    disconnectNumberDTO: DisconnectNumberDTO
  ): IO[ServerError, Boolean] =
    transact {
      contactNumberRepository.disconnectNumber(id = disconnectNumberDTO.id)
    }
//
//  override def deleteContactNumber(
//      deleteContactNumberDTO: DeleteContactNumberDTO
//  ): Task[ContactNumber] = ???

object ContactNumberServiceLive:
  lazy val layer: ZLayer[
    TwilioClient with ContactNumberRepository,
    Throwable,
    ContactNumberService
  ] =
    ZLayer.fromFunction(ContactNumberServiceLive(_, _))
