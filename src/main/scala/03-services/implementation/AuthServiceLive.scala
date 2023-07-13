package services.implementation

import models.dto.{ Auth, AuthData }
import models.enums.UserType
import clients.JwtClient
import services.AuthService
import zio.*
import zio.http.Request

import java.util.UUID
import java.time.Instant

class AuthServiceLive(jwtClient: JwtClient) extends AuthService:
  import AuthService._

  override def extractBearer(req: Request): Task[String] =
    ZIO.fromOption(req.bearerToken).orElseSucceed(" ")

  override def isAuth(isAuthDTO: IsAuthDTO): Task[Auth] =
    jwtClient
      .decodeToken(isAuthDTO.token)
      .fold(
        _ ⇒ Auth(UUID.randomUUID(), false, UserType.CUSTOMER),
        {
          case Some(auth) ⇒ auth
          case _          ⇒ Auth(UUID.randomUUID(), false, UserType.CUSTOMER)
        }
      )

  override def authByLevel(authByLevelDTO: AuthByLevelDTO): UIO[Boolean] =
    ZIO.succeed {
      authByLevelDTO.auth match
        case auth if !auth.isAuth                     ⇒ false
        case auth if auth.authLevel == UserType.ADMIN ⇒ true
        case auth if !authByLevelDTO.authLevels.contains(auth.authLevel) ⇒
          false
        case _ ⇒ true
    }

  override def authById(authByIdDTO: AuthByIdDTO): UIO[Boolean] =
    ZIO.succeed {
      authByIdDTO.auth match
        case auth if !auth.isAuth                     ⇒ false
        case auth if auth.authLevel == UserType.ADMIN ⇒ true
        case auth if auth.loginId != authByIdDTO.id   ⇒ false
        case _                                        ⇒ true
    }

object AuthServiceLive:
  lazy val layer: ZLayer[JwtClient, Throwable, AuthService] =
    ZLayer.fromFunction(AuthServiceLive(_))
