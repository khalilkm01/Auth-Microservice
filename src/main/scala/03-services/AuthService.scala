package services

import models.dto.{ Auth, AuthData }
import models.enums.UserType
import zio.http.Request
import zio.{ Task, UIO }

import java.util.UUID

trait AuthService:
  import AuthService._
  def extractBearer(req: Request): Task[String]
  def isAuth(isAuthDTO: IsAuthDTO): Task[Auth]
  def authByLevel(authByLevelDTO: AuthByLevelDTO): UIO[Boolean]
  def authById(authByIdDTO: AuthByIdDTO): UIO[Boolean]

object AuthService:
  case class IsAuthDTO(token: String)
  case class AuthByLevelDTO(auth: Auth, authLevels: List[UserType])
  case class AuthByIdDTO(auth: Auth, id: UUID)
