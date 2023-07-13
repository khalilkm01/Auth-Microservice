package gateway.api

import config.Config.ServerConfig as AppServerConfig
import models.dto.{ Auth, AuthData }
import models.common.ServerError
import services.{ AuthService, ContactNumberService, EmailService, LoginService }
import gateway.Gateway
import implementation.{ AuthApi, ContactNumberApi, EmailApi, LoginApi }

import zio._
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.JsonEncoder

final case class ApiGateway()
    extends ApiGateway.GatewayOut
    with AuthApi
    with LoginApi
    with EmailApi
    with ContactNumberApi:

  override val startingMessage: String = "Starting API Gateway..."

  private val HEALTH_CHECK: UHttpApp = Http.collectZIO[Request] { case Method.GET -> !! / "health" ⇒
    ZIO.log("health check") *>
      ZIO.succeed(Response.ok)
  }

  private val API_ROUTE: App[Services] =
    AuthAPI ++ EmailAPI
      ++ LoginAPI ++ ContactNumberAPI
      ++ HEALTH_CHECK

  override def start: RIO[Server with Services, Unit] =
    ZIO.logInfo(startingMessage) *>
      Server
        .serve(API_ROUTE) *> ZIO.logInfo("Gateway Closed..")

object ApiGateway:
  type Services              = AuthService with ContactNumberService with EmailService with LoginService
  private type EnvironmentIn = Server with Services
  private type GatewayOut = Gateway[
    EnvironmentIn
  ]
  lazy val layer: RLayer[EnvironmentIn, ApiGateway] = ZLayer.succeed(ApiGateway())
