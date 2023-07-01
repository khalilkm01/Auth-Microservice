package clients

import config.TestConfig
import models.enums.CountryCode

import zio.*
import zio.test.*
import zio.test.Assertion.*

object TwilioClientSpec extends ZIOSpecDefault:

  private lazy val layer: TaskLayer[TwilioClient] =
    TestConfig.twilioConfig >+> ZLayer.fromFunction(implementation.TwilioClientLive(_))

  def spec: Spec[Any, Throwable] = suite("TwilioClientSpec")(
    test("send sms") {
      for
        twilioClient <- ZIO.service[TwilioClient]
        _            <- twilioClient.requestPhoneCode(CountryCode.GB, "7123456789")
      yield assertCompletes
    }
  ).provide(layer)
