package clients

import config.TestConfig
import models.common.Auth
import models.enums.UserType

import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID

object JwtClientSpec extends ZIOSpecDefault:

  private lazy val layer: TaskLayer[JwtClient] =
    TestConfig.authConfig >+> ZLayer.fromFunction(implementation.JwtClientLive(_))

  val auth: Auth = Auth(UUID.randomUUID(), true, UserType.ADMIN)
  def spec: Spec[Any, Any] = suite("JwtClientSpec")(
    test("JwtClient should be able to generate a token") {
      for
        jwtClient <- ZIO.service[JwtClient]
        token     <- jwtClient.generateToken(auth)
      yield assert(token.token)(Assertion.isNonEmptyString)
    },
    test("decodeToken should return Some(Auth) for a valid token") {

      for {
        jwtClient <- ZIO.service[JwtClient]
        token     <- jwtClient.generateToken(auth)
        result    <- jwtClient.decodeToken(token.token)
      } yield assertTrue(result.get == auth)
    },
    test("decodeToken should return None for an invalid token") {
      val token = "invalid_token"
      for {
        jwtClient <- ZIO.service[JwtClient]
        result    <- jwtClient.decodeToken(token).catchAll(_ => ZIO.succeed(None))
      } yield assertTrue(result.isEmpty)
    },
    test("generateToken should generate a valid token") {

      for {
        jwtClient <- ZIO.service[JwtClient]
        result    <- jwtClient.generateToken(auth)
        decoded   <- jwtClient.decodeToken(result.token)
      } yield assertTrue(decoded.get == auth)
    }
  ).provide(layer)
