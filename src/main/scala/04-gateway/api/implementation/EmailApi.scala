package gateway.api.implementation

import models.infrastructure.*
import models.infrastructure.Query.given
import services.EmailService
import EmailService.*
import zio.ZIO
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*

import java.util.UUID
trait EmailApi extends HttpHelper:

  protected val EmailAPI: App[EmailService] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / ROOT / "checkEmailAvailable" ⇒
        httpResponseHandler[EmailService, Boolean](
          for {
            query <- extractQuery[CheckEmailAvailableQuery](req)
            emailAddress = query.emailAddress
            user         = query.user
            available <- ZIO.serviceWithZIO[EmailService](_.isEmailUsable(IsEmailUsableDTO(emailAddress, user)))
          } yield available
        )
      case req @ Method.POST -> !! / ROOT / "setupEmail" ⇒
        httpResponseHandler[EmailService, UUID](
          for {
            query <- extractQuery[SetupEmailQuery](req)
            emailAddress = query.emailAddress
            user         = query.user
            emailId <- ZIO.serviceWithZIO[EmailService](_.createEmail(CreateEmailDTO(emailAddress, user)))
          } yield emailId
        )

    }
