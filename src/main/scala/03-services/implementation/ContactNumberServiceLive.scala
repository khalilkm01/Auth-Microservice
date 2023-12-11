package services.implementation

import models.enums.CountryCode
import models.common.ServerError
import models.persisted.ContactNumber
import models.infrastructure._
import models.infrastructure.Event.given
import publisher.EventPublisher
import clients.TwilioClient
import repositories.ContactNumberRepository
import services.ContactNumberService

import zio.*
import zio.json._
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource

class ContactNumberServiceLive(
  twilioClient: TwilioClient,
  contactNumberRepository: ContactNumberRepository,
  eventPublisher: EventPublisher
) extends ContactNumberService
    with ServiceAssistant:

  import ContactNumberService._

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
    for
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
              userType = createContactNumberDTO.user,
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
      _ <- eventPublisher.publishEvent(
        contactNumber.id,
        ContactNumberCreatedEvent(contactNumberId = contactNumber.id, user = contactNumber.userType)
      )
    yield contactNumber
  }

  override def verifyPhoneCode(
    verifyPhoneCodeDTO: VerifyPhoneCodeDTO
  ): IO[ServerError, Boolean] = transact {
    for
      contactNumber <- contactNumberRepository.getById(verifyPhoneCodeDTO.id)
      verify <- twilioClient
        .verifyPhoneCode(
          contactNumber.countryCode,
          contactNumber.digits,
          verifyPhoneCodeDTO.code
        )
    yield verify
  }

  override def connectNumber(
    connectNumberDTO: ConnectNumberDTO
  ): IO[ServerError, Boolean] = transact {
    for
      verify <- verifyPhoneCode(
        VerifyPhoneCodeDTO(
          id = connectNumberDTO.contactNumberId,
          code = connectNumberDTO.code,
          user = connectNumberDTO.user
        )
      )
      connected <-
        if verify then
          for
            connected <- contactNumberRepository.connectNumber(id = connectNumberDTO.contactNumberId)
            _ <- eventPublisher.publishEvent(
              connectNumberDTO.contactNumberId,
              ContactNumberConnectedEvent(
                contactNumberId = connectNumberDTO.contactNumberId,
                user = connectNumberDTO.user
              ).toJson
            )
          yield connected
        else
          ZIO.fail(
            ServerError.ServiceError(
              ServerError.ServiceErrorMessage.InternalServiceError("Phone code verification failed")
            )
          )
    yield connected
  }
  override def disconnectNumber(
    disconnectNumberDTO: DisconnectNumberDTO
  ): IO[ServerError, Boolean] =
    transact {
      for
        disconnected <- contactNumberRepository.disconnectNumber(id = disconnectNumberDTO.id)

        _ <- eventPublisher.publishEvent(
          disconnectNumberDTO.id,
          ContactNumberDisconnectedEvent(contactNumberId = disconnectNumberDTO.id).toJson
        )
      yield disconnected
    }
//
//  override def deleteContactNumber(
//      deleteContactNumberDTO: DeleteContactNumberDTO
//  ): Task[ContactNumber] = ???

object ContactNumberServiceLive:

  lazy val layer: ZLayer[
    TwilioClient with ContactNumberRepository with EventPublisher,
    Throwable,
    ContactNumberService
  ] =
    ZLayer.fromFunction(ContactNumberServiceLive(_, _, _))
