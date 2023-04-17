package services.implementation

import config.Config.AuthConfig
import models.common.{ Auth, AuthData, ServerError }
import models.enums.UserType
import models.persisted.Login
import clients.JwtClient
import services.LoginService
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import repositories.quill.QuillContext
import zio.*
import com.github.t3hnar.bcrypt.*
import org.joda.time.DateTime
import io.getquill.context.ZioJdbc.QIO

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class LoginServiceLive(
  jwtClient: JwtClient,
  loginRepository: LoginRepository,
  contactNumberRepository: ContactNumberRepository,
  emailRepository: EmailRepository,
  authConfig: AuthConfig
) extends LoginService
    with ServiceAssistant:

  import LoginService._
  import QuillContext._

  private val dataSource: ULayer[DataSource] = dataSourceLayer

  override def createLogin(
    createLoginDTO: CreateLoginDTO
  ): IO[ServerError, Login] =
    transaction {
      for {
        now <- ZIO.succeed(DateTime.now)
        email <- emailRepository
          .connectEmail(createLoginDTO.emailId)
        contactNumber <- contactNumberRepository
          .connectNumber(createLoginDTO.contactNumberId)
        newLogin <-
          if (contactNumber && email)
            loginRepository
              .create(
                Login(
                  id = UUID.randomUUID,
                  password = createLoginDTO.password.bcryptBounded(authConfig.private_key),
                  blocked = false,
                  user = createLoginDTO.user,
                  emailId = createLoginDTO.emailId,
                  contactNumberId = createLoginDTO.contactNumberId,
                  now,
                  now
                )
              )
          else
            ZIO.fail(
              ServerError.ServiceError(
                ServerError.ServiceErrorMessage.IllegalServiceCall
              )
            )
      } yield newLogin
    }.provide(dataSource).catchAll(handleError)

  override def updateLoginPassword(
    updateLoginPasswordDTO: UpdateLoginPasswordDTO
  ): Task[Login] = transaction {
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
          loginRepository.save(
            loginModel.copy(
              password = newPassword,
              updatedAt = dateTime
            )
          )
        else
          ZIO.fail(
            ServerError.ServiceError(
              ServerError.ServiceErrorMessage
                .UnauthorizedAccessError(loginModel.id)
            )
          )

    } yield login
  }.provide(dataSource).catchAll(handleError)

  override def loginByEmail(
    loginByEmailDTO: LoginByEmailDTO
  ): IO[ServerError, AuthData] =
    transaction {
      for {
        email <- emailRepository.getByEmailAddress(emailAddress = loginByEmailDTO.email, user = loginByEmailDTO.user)
        login <- loginRepository.getByEmailId(email.id)
        compare <- ZIO
          .fromTry(loginByEmailDTO.password.isBcryptedSafeBounded(login.password))
        authData <-
          if (compare)
            jwtClient.generateToken(Auth(login.id, compare, loginByEmailDTO.user))
          else
            ZIO.fail(ServerError.ServiceError(ServerError.ServiceErrorMessage.LoginError))

      } yield authData

    }.provide(dataSource).catchAll(handleError)

object LoginServiceLive:
  lazy val layer: ZLayer[
    JwtClient with LoginRepository with EmailRepository with ContactNumberRepository with AuthConfig,
    Throwable,
    LoginService
  ] =
    ZLayer.fromFunction(LoginServiceLive(_, _, _, _, _))
