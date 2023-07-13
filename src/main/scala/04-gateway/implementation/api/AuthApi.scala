package gateway.implementation.api

import models.dto.{ Auth, AuthData }
import models.common.ServerError
import models.infrastructure._
import models.infrastructure.Query.given
import services.AuthService
import AuthService._

import zio.ZIO
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*
trait AuthApi extends HttpHelper:

  protected val AuthAPI: App[AuthService] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / ROOT / "isAuth" ⇒
        httpResponseHandler[AuthService, Auth](
          for {
            query <- extractQuery[IsAuthQuery](req)
            token = query.token
            auth <- ZIO.serviceWithZIO[AuthService](_.isAuth(IsAuthDTO(token)))
          } yield auth
        )
      case req @ Method.GET -> !! / ROOT / "authByLevel" ⇒
        httpResponseHandler[AuthService, Boolean](
          for {
            query <- extractQuery[AuthByLevelQuery](req)
            auth       = query.auth
            authLevels = query.authLevels
            auth <- ZIO.serviceWithZIO[AuthService](_.authByLevel(AuthByLevelDTO(auth, authLevels)))
          } yield auth
        )
      case req @ Method.GET -> !! / ROOT / "authById" ⇒
        httpResponseHandler[AuthService, Boolean](
          for {
            query <- extractQuery[AuthByIdQuery](req)
            auth   = query.auth
            authId = query.id
            auth <- ZIO.serviceWithZIO[AuthService](_.authById(AuthByIdDTO(auth, authId)))
          } yield auth
        )
    }
