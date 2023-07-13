package gateway.api.implementation

import models.infrastructure._
import models.infrastructure.Query.given
import services.EmailService
import EmailService._

import zio.ZIO
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*
trait EmailApi extends HttpHelper:

  protected val EmailAPI: App[EmailService] =
    Http.collectZIO[Request] { case req @ Method.GET -> !! / ROOT / "checkEmailAvailable" ⇒
      httpResponseHandler[EmailService, Boolean](
        for {
          query <- extractQuery[CheckEmailAvailableQuery](req)
          emailAddress = query.emailAddress
          user         = query.user
          available <- ZIO.serviceWithZIO[EmailService](_.isEmailUsable(IsEmailUsableDTO(emailAddress, user)))
        } yield available
      )

    }
