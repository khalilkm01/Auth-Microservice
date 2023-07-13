package services.implementation

import config.Config.AuthConfig
import models.dto.{ Auth, AuthData }
import models.common.ServerError
import models.enums.UserType
import models.persisted.Login
import clients.{ JwtClient, TwilioClient }
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import repositories.quill.QuillContext
import services.LoginService
import services.implementation.contactNumber.ContactNumberHelper
import services.implementation.email.EmailHelper
import services.implementation.login.LoginHelper
import zio.*
import com.github.t3hnar.bcrypt.*
import org.joda.time.DateTime
import io.getquill.context.ZioJdbc.QIO

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class LoginServiceLive(
  jwtClient: JwtClient,
  twilioClient: TwilioClient,
  loginRepository: LoginRepository,
  contactNumberRepository: ContactNumberRepository,
  emailRepository: EmailRepository,
  authConfig: AuthConfig
) extends LoginService
    with ContactNumberHelper
    with EmailHelper
    with LoginHelper
    with ServiceAssistant:

  import LoginService._
  import QuillContext._

  private val dataSource: ULayer[DataSource] = dataSourceLayer

  override def createLogin(
    createLoginDTO: CreateLoginDTO
  ): IO[ServerError, AuthData] =
    transact {
      for
        contactNumber <-
          contactNumberRepository
            .getByNumber(
              countryCode = createLoginDTO.countryCode,
              digits = createLoginDTO.digits,
              user = createLoginDTO.user
            )

        emailId <-
          createEmailHelper(
            emailRepository = emailRepository,
            emailAddress = createLoginDTO.emailAddress,
            user = createLoginDTO.user
          )

        _ <- connectNumberHelper(
          id = contactNumber.id,
          code = createLoginDTO.code,
          user = createLoginDTO.user,
          twilioClient = twilioClient,
          contactNumberRepository = contactNumberRepository
        )

        _ <- createUserHelper(
          contactNumberId = contactNumber.id,
          emailId = emailId,
          password = createLoginDTO.password,
          user = createLoginDTO.user,
          emailRepository = emailRepository,
          contactNumberRepository = contactNumberRepository,
          loginRepository = loginRepository,
          authConfig = authConfig
        )

        loginUser <-
          loginByEmailHelper(
            emailAddress = createLoginDTO.emailAddress,
            password = createLoginDTO.password,
            user = createLoginDTO.user,
            loginRepository = loginRepository,
            emailRepository = emailRepository,
            jwtClient = jwtClient
          )
      yield loginUser
    }

  override def updateLoginPassword(
    updateLoginPasswordDTO: UpdateLoginPasswordDTO
  ): IO[ServerError, Boolean] = transact {
    for {
      dateTime    <- ZIO.succeed(DateTime.now())
      loginModel  <- loginRepository.getById(updateLoginPasswordDTO.loginId)
      newPassword <- ZIO.fromTry(updateLoginPasswordDTO.newPassword.bcryptSafeBounded(authConfig.private_key))
      compare <- ZIO
        .fromTry(
          updateLoginPasswordDTO.currentPassword.isBcryptedSafeBounded(loginModel.password)
        )
      login <-
        if (compare)
          loginRepository
            .save(
              loginModel.copy(
                password = newPassword,
                updatedAt = dateTime
              )
            )
            .as(true)
        else
          ZIO.fail(
            ServerError.ServiceError(
              ServerError.ServiceErrorMessage
                .UnauthorizedAccessError(loginModel.id)
            )
          )

    } yield login
  }

  override def loginByEmail(
    loginByEmailDTO: LoginByEmailDTO
  ): IO[ServerError, AuthData] =
    transact {
      loginByEmailHelper(
        emailAddress = loginByEmailDTO.emailAddress,
        password = loginByEmailDTO.password,
        user = loginByEmailDTO.user,
        loginRepository = loginRepository,
        emailRepository = emailRepository,
        jwtClient = jwtClient
      )
    }

object LoginServiceLive:
  lazy val layer: ZLayer[
    JwtClient with TwilioClient with LoginRepository with EmailRepository with ContactNumberRepository with AuthConfig,
    Throwable,
    LoginService
  ] =
    ZLayer.fromFunction(LoginServiceLive(_, _, _, _, _, _))
