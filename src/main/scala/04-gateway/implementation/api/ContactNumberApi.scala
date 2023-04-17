package gateway.implementation.api

import models.common.ServerError
import models.persisted.ContactNumber
import models.infrastructure._
import models.infrastructure.Query.given
import services.ContactNumberService
import ContactNumberService._

import zio.ZIO
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*

trait ContactNumberApi extends HttpHelper:

  protected val ContactNumberAPI: App[ContactNumberService] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / ROOT / "requestPhoneCode" ⇒
        httpResponseHandler[ContactNumberService, Boolean](
          for {
            query <- extractQuery[RequestPhoneCodeQuery](req)
            countryCode = query.countryCode
            digits      = query.digits
            requested <- ZIO.serviceWithZIO[ContactNumberService](
              _.requestPhoneCode(RequestPhoneCodeDTO(countryCode = countryCode, digits = digits))
            )
          } yield requested
        )
      case req @ Method.POST -> !! / ROOT / "setupContactNumber" ⇒
        httpResponseHandler[ContactNumberService, ContactNumber](
          for {
            query <- extractQuery[SetupContactNumberQuery](req)
            countryCode = query.countryCode
            digits      = query.digits
            user        = query.user
            contactNumber <- ZIO.serviceWithZIO[ContactNumberService](
              _.createContactNumber(CreateContactNumberDTO(countryCode = countryCode, digits = digits, user = user))
            )

          } yield contactNumber
        )

    }
