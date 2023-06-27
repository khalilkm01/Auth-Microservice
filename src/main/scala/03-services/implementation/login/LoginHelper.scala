package services.implementation.login

import clients.JwtClient
import models.common.{ Auth, AuthData, ServerError }
import models.enums.UserType
import models.persisted.Login
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import zio.{ RIO, ZIO }
import org.joda.time.DateTime
import com.github.t3hnar.bcrypt.*
import config.Config.AuthConfig

import java.util.UUID
import javax.sql.DataSource

trait LoginHelper:
  protected def createUserHelper(
    emailId: UUID,
    contactNumberId: UUID,
    password: String,
    user: UserType,
    emailRepository: EmailRepository,
    contactNumberRepository: ContactNumberRepository,
    loginRepository: LoginRepository,
    authConfig: AuthConfig
  ): RIO[DataSource, UUID] =
    for {
      now <- ZIO.succeed(DateTime.now())
      email <- emailRepository
        .connectEmail(emailId)
      contactNumber <- contactNumberRepository
        .connectNumber(contactNumberId)
      newLogin <-
        if (contactNumber && email)
          loginRepository
            .create(
              Login(
                id = UUID.randomUUID,
                password = password.bcryptBounded(authConfig.private_key),
                blocked = false,
                userType = user,
                emailId = emailId,
                contactNumberId = contactNumberId,
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
    } yield newLogin.id

  protected def loginByEmailHelper(
    loginRepository: LoginRepository,
    emailRepository: EmailRepository,
    jwtClient: JwtClient,
    emailAddress: String,
    password: String,
    user: UserType
  ): RIO[DataSource, AuthData] = for {
    email <- emailRepository.getByEmailAddress(emailAddress = emailAddress, user = user)
    login <- loginRepository.getByEmailId(email.id)
    compare <- ZIO
      .fromTry(password.isBcryptedSafeBounded(login.password))
    authData <-
      if (compare)
        jwtClient.generateToken(Auth(loginId = login.id, isAuth = compare, authLevel = user))
      else
        ZIO.fail(ServerError.ServiceError(ServerError.ServiceErrorMessage.LoginError))

  } yield authData
