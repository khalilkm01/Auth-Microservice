package services.implementation

import config.Config.AuthConfig
import models.dto.{ Auth, AuthData }
import models.common.ServerError
import models.enums.UserType
import models.persisted.Login
import models.infrastructure._
import models.infrastructure.Event.given
import publisher.EventPublisher
import clients.{ JwtClient, TwilioClient }
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import repositories.quill.QuillContext
import services.LoginService

import zio.*
import zio.json._
import com.github.t3hnar.bcrypt.*
import org.joda.time.DateTime
import io.getquill.context.ZioJdbc.QIO

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class LoginServiceLive(
  jwtClient: JwtClient,
  twilioClient: TwilioClient,
  eventPublisher: EventPublisher,
  loginRepository: LoginRepository,
  contactNumberRepository: ContactNumberRepository,
  emailRepository: EmailRepository,
  authConfig: AuthConfig
) extends LoginService
    with ServiceAssistant:

  import LoginService._
  import QuillContext._

  override def createLogin(
    createLoginDTO: CreateLoginDTO
  ): IO[ServerError, AuthData] =
    transact {
      for
        now <- ZIO.succeed(DateTime.now())
        contactNumber <-
          contactNumberRepository
            .getByNumber(
              countryCode = createLoginDTO.countryCode,
              digits = createLoginDTO.digits,
              user = createLoginDTO.user
            )
        email <-
          emailRepository.getByEmailAddress(emailAddress = createLoginDTO.emailAddress, user = createLoginDTO.user)

        _ <- loginRepository.create(
          Login(
            id = createLoginDTO.loginId,
            password = createLoginDTO.password.bcryptBounded(authConfig.private_key),
            blocked = false,
            userType = createLoginDTO.user,
            emailId = email.id,
            contactNumberId = contactNumber.id,
            now,
            now
          )
        )

        loginUser <-
          jwtClient.generateToken(
            Auth(loginId = createLoginDTO.loginId, isAuth = true, authLevel = createLoginDTO.user)
          )

        _ <- eventPublisher.publishEvent(
          KafkaMessage(
            createLoginDTO.loginId,
            LoginCreatedEvent(
              loginId = createLoginDTO.loginId,
              contactNumberId = contactNumber.id,
              emailId = email.id,
              user = createLoginDTO.user,
              code = createLoginDTO.code
            ).toJson
          ),
          EventTopics.LOGIN_TOPIC
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
      _ <- eventPublisher.publishEvent(
        KafkaMessage(
          loginModel.id,
          LoginPasswordUpdatedEvent(
            loginId = loginModel.id,
            user = loginModel.userType
          ).toJson
        ),
        EventTopics.LOGIN_TOPIC
      )

    } yield login
  }

  override def loginByEmail(
    loginByEmailDTO: LoginByEmailDTO
  ): IO[ServerError, AuthData] =
    transact {
      for
        email <- emailRepository.getByEmailAddress(
          emailAddress = loginByEmailDTO.emailAddress,
          user = loginByEmailDTO.user
        )
        login <- loginRepository.getByEmailId(email.id)
        compare <- ZIO
          .fromTry(loginByEmailDTO.password.isBcryptedSafeBounded(login.password))
        authData <-
          if (compare)
            jwtClient.generateToken(Auth(loginId = login.id, isAuth = compare, authLevel = loginByEmailDTO.user))
          else
            ZIO.fail(ServerError.ServiceError(ServerError.ServiceErrorMessage.LoginError))
      yield authData
    }

object LoginServiceLive:
  lazy val layer: ZLayer[
    JwtClient
      with TwilioClient
      with EventPublisher
      with LoginRepository
      with EmailRepository
      with ContactNumberRepository
      with AuthConfig,
    Throwable,
    LoginService
  ] =
    ZLayer.fromFunction(LoginServiceLive(_, _, _, _, _, _, _))
