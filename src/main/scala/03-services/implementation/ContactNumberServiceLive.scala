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

import java.util.UUID
import javax.sql.DataSource

class ContactNumberServiceLive(
  twilioClient: TwilioClient,
  contactNumberRepository: ContactNumberRepository
) extends ContactNumberService
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
      createdAt <- ZIO.succeed(DateTime.now)
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
    for {
      contactNumber <- contactNumberRepository.getById(verifyPhoneCodeDTO.id)
      verify <- twilioClient.verifyPhoneCode(
        contactNumber.countryCode,
        contactNumber.digits,
        verifyPhoneCodeDTO.code
      )
    } yield verify
  }

  override def connectNumber(
    connectNumberDTO: ConnectNumberDTO
  ): IO[ServerError, Boolean] = transact {
    verifyPhoneCode(
      VerifyPhoneCodeDTO(
        id = connectNumberDTO.id,
        code = connectNumberDTO.code,
        user = connectNumberDTO.user
      )
    ).flatMap {
      case true =>
        contactNumberRepository.connectNumber(id = connectNumberDTO.id)
      case false =>
        ZIO.fail(
          ServerError.ServiceError(
            ServerError.ServiceErrorMessage.InternalServiceError("Phone code verification failed")
          )
        )
    }
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
