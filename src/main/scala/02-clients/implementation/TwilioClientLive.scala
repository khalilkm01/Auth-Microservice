package clients.implementation

import config.Config.TwilioConfig
import clients.TwilioClient
import models.enums.CountryCode
import zio.http.model.Headers.Header
import zio.{ Task, URLayer, ZLayer }

final case class TwilioClientLive(twilioConfig: TwilioConfig)
    extends TwilioClient
    with HttpClient(root = twilioConfig.root, headers = Header("Authorization", s"Basic ${twilioConfig.auth_token}")):
  override def verifyPhoneCode(countryCode: CountryCode, digits: String, code: String): Task[Boolean] = ???

  override def requestPhoneCode(countryCode: CountryCode, digits: String): Task[Boolean] = ???

object TwilioClientLive:
  lazy val layer: URLayer[TwilioConfig, TwilioClient] = ZLayer.fromFunction(TwilioClientLive(_))
