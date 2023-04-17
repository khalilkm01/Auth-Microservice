package gateway.implementation.api

import models.common.{ AuthData, ServerError }
import models.enums.{ CountryCode, UserType }
import models.persisted.Login
import models.infrastructure.*
import models.infrastructure.Query.given
import services.{ AuthService, ContactNumberService, EmailService, LoginService }
import LoginService.*
import EmailService.*
import ContactNumberService.*
import AuthService.*
import zio.ZIO
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*
trait LoginApi extends HttpHelper:

  protected val LoginAPI: App[LoginService with AuthService with ContactNumberService with EmailService] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / ROOT / "loginUser" ⇒
        httpResponseHandler[LoginService, AuthData](
          for {
            query <- extractQuery[LoginUserQuery](req)
            emailAddress = query.emailAddress
            password     = query.password
            user         = query.user
            login <- ZIO.serviceWithZIO[LoginService](_.loginByEmail(LoginByEmailDTO(emailAddress, password, user)))
          } yield login
        )
      case req @ Method.POST -> !! / ROOT / "updatePassword" ⇒
        httpResponseHandler[LoginService with AuthService, Boolean](
          for {
            bearer: String <- ZIO.serviceWithZIO[AuthService](_.extractBearer(req))
            auth           <- ZIO.serviceWithZIO[AuthService](_.isAuth(IsAuthDTO(bearer)))
            query          <- extractQuery[UpdatePasswordQuery](req)

            loginId         = query.loginId
            currentPassword = query.currentPassword
            newPassword     = query.newPassword

            authById <- ZIO.serviceWithZIO[AuthService](_.authById(AuthByIdDTO(auth, query.loginId)))

            updatePassword <-
              if authById then
                ZIO
                  .serviceWithZIO[LoginService](
                    _.updateLoginPassword(
                      UpdateLoginPasswordDTO(
                        loginId = loginId,
                        currentPassword = currentPassword,
                        newPassword = newPassword
                      )
                    )
                  )
                  .as(true)
              else ZIO.fail(ServerError.NotFoundError("ILLEGAL SERVICE CALL"))

          } yield updatePassword
        )
      case req @ Method.POST -> !! / ROOT / "createUser" ⇒
        httpResponseHandler[LoginService with AuthService with ContactNumberService with EmailService, AuthData](
          for {
            bearer: String <- ZIO.serviceWithZIO[AuthService](_.extractBearer(req))
            auth           <- ZIO.serviceWithZIO[AuthService](_.isAuth(IsAuthDTO(bearer)))
            query          <- extractQuery[CreateLoginQuery](req)
            emailAddress = query.emailAddress
            password     = query.password
            user         = query.user
            code         = query.code
            countryCode <- CountryCode.toCountryCode(query.countryCode)
            digits = query.digits

            levelsRequired: List[UserType] = user match
              case UserType.DOCTOR   ⇒ List(UserType.ADMIN, UserType.DOCTOR)
              case UserType.CUSTOMER ⇒ List(UserType.ADMIN, UserType.CUSTOMER)
              case UserType.ADMIN    ⇒ List(UserType.ADMIN)
            authLevel: Boolean <- ZIO.serviceWithZIO[AuthService](_.authByLevel(AuthByLevelDTO(auth, levelsRequired)))

            // Check if the user is allowed to create a login
            contactNumber <-
              if authLevel then
                ZIO
                  .serviceWithZIO[ContactNumberService](
                    _.findNumber(FindNumberDTO(countryCode = countryCode, digits = digits, user = user))
                  )
              else ZIO.fail(ServerError.NotFoundError("ILLEGAL SERVICE CALL"))
            email <- ZIO
              .serviceWithZIO[EmailService](
                _.createEmail(
                  CreateEmailDTO(
                    emailAddress = emailAddress,
                    user = user
                  )
                )
              )

            _ <- ZIO
              .serviceWithZIO[ContactNumberService](
                _.connectNumber(ConnectNumberDTO(id = contactNumber.id, code = code, user = user))
              )
            _ <- ZIO
              .serviceWithZIO[LoginService](
                _.createLogin(
                  CreateLoginDTO(
                    contactNumberId = contactNumber.id,
                    emailId = email.id,
                    password = password,
                    user = user
                  )
                )
              )
              .tapError(_ ⇒
                {
                  for {
                    _ <- ZIO.serviceWithZIO[EmailService](_.deleteEmail(DeleteEmailDTO(email.id)))
                    _ <- ZIO
                      .serviceWithZIO[ContactNumberService](_.disconnectNumber(DisconnectNumberDTO(contactNumber.id)))
                  } yield ()
                }.tapError { _ ⇒
                  ZIO.logError(
                    s"Need to manually disconnect contact Number with id: ${contactNumber.id}, and delete email with ${email.id}"
                  )
                }
              ) // Also handles having to delete email and disconnect contact number if login creation fails

            loginUser <-
              ZIO
                .serviceWithZIO[LoginService](_.loginByEmail(LoginByEmailDTO(emailAddress, password, user)))

          } yield loginUser
        )
    }
