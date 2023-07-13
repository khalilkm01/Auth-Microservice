package clients.implementation

import config.Config.AuthConfig
import models.dto.{ Auth, AuthData }
import clients.JwtClient
import org.joda.time.DateTime
import pdi.jwt.algorithms.JwtHmacAlgorithm
import zio.{ Task, URLayer, ZIO, ZLayer }
import zio.json.*
import pdi.jwt.{ JwtAlgorithm, JwtClaim, JwtZIOJson }

final case class JwtClientLive(authConfig: AuthConfig) extends JwtClient:
  import Auth.given
  import AuthData.given

  private val ALGORITHM: JwtHmacAlgorithm = JwtAlgorithm.HS256
  private val SECRET_KEY: String          = authConfig.private_key

  override def decodeToken(token: String): Task[Option[Auth]] =
    for {
      claim   <- ZIO.fromTry(JwtZIOJson.decode(token, SECRET_KEY, Seq(ALGORITHM)))
      content <- ZIO.succeed(claim.content)
      auth    <- ZIO.succeed(content.fromJson[Auth].toOption)
    } yield auth

  override def generateToken(auth: Auth): Task[AuthData] =
    for {
      now <- ZIO.succeed(DateTime.now())
      claim <- ZIO.succeed(
        JwtClaim(
          content = auth.toJson,
          expiration = Some(now.plusYears(1).getMillis),
          issuedAt = Some(now.getMillis)
        )
      )
      token <- ZIO.succeed(JwtZIOJson.encode(claim, SECRET_KEY, ALGORITHM))
    } yield AuthData(
      loginId = auth.loginId,
      token = token,
      tokenExpiration = DateTime(claim.expiration.get),
      authLevel = auth.authLevel
    )

object JwtClientLive:
  lazy val layer: URLayer[AuthConfig, JwtClient] = ZLayer.fromFunction(JwtClientLive(_))
