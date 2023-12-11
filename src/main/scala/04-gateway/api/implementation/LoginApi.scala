package gateway.api.implementation

import models.dto.AuthData
import models.common.ServerError
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

  protected val LoginAPI: App[LoginService with AuthService] =
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
              else ZIO.fail(ServerError.NotFoundError("ILLEGAL SERVICE CALL"))

          } yield updatePassword
        )
      case req @ Method.POST -> !! / ROOT / "createUser" ⇒
        httpResponseHandler[LoginService with AuthService, AuthData](
          for {
            bearer: String <- ZIO.serviceWithZIO[AuthService](_.extractBearer(req))
            auth           <- ZIO.serviceWithZIO[AuthService](_.isAuth(IsAuthDTO(bearer)))
            query          <- extractQuery[CreateLoginQuery](req)

            emailAddress = query.emailAddress
            password     = query.password
            user         = query.user
            code         = query.code
            digits       = query.digits
            loginId      = query.loginId
            countryCode <- CountryCode.toCountryCode(query.countryCode)

            levelsRequired: List[UserType] = user match
              case UserType.DOCTOR   ⇒ List(UserType.ADMIN, UserType.DOCTOR)
              case UserType.CUSTOMER ⇒ List(UserType.ADMIN, UserType.CUSTOMER)
              case UserType.ADMIN    ⇒ List(UserType.ADMIN)

            authLevel: Boolean <- ZIO.serviceWithZIO[AuthService](_.authByLevel(AuthByLevelDTO(auth, levelsRequired)))

            createUser <-
              if authLevel then
                ZIO
                  .serviceWithZIO[LoginService](
                    _.createLogin(
                      CreateLoginDTO(
                        loginId = loginId,
                        emailAddress = emailAddress,
                        password = password,
                        user = user,
                        code = code,
                        countryCode = countryCode,
                        digits = digits
                      )
                    )
                  )
              else ZIO.fail(ServerError.NotFoundError("ILLEGAL SERVICE CALL"))
          } yield createUser
        )
    }
