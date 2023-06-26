package services.implementation.contactNumber

import models.persisted.ContactNumber
import clients.TwilioClient
import models.common.ServerError
import models.enums.{ CountryCode, UserType }
import repositories.ContactNumberRepository
import services.ContactNumberService
import zio.{ RIO, ZIO }
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource
trait ContactNumberHelper:

  protected def verifyPhoneCodeHelper(
    id: UUID,
    code: String,
    user: UserType,
    contactNumberRepository: ContactNumberRepository,
    twilioClient: TwilioClient
  ): RIO[DataSource, Boolean] = for {
    contactNumber <- contactNumberRepository.getById(id)
    verify <- twilioClient.verifyPhoneCode(
      contactNumber.countryCode,
      contactNumber.digits,
      code
    )
  } yield verify
  protected def connectNumberHelper(
    contactNumberRepository: ContactNumberRepository,
    twilioClient: TwilioClient,
    id: UUID,
    code: String,
    user: UserType
  ): RIO[DataSource, Boolean] = verifyPhoneCodeHelper(
    id = id,
    code = code,
    user = user,
    twilioClient = twilioClient,
    contactNumberRepository = contactNumberRepository
  ).flatMap {
    case true =>
      contactNumberRepository.connectNumber(id = id)
    case false =>
      ZIO.fail(
        ServerError.ServiceError(
          ServerError.ServiceErrorMessage.InternalServiceError("Phone code verification failed")
        )
      )
  }
