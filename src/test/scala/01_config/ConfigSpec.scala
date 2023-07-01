package config

import Config._

import zio.ZIO
import zio.test._

object ConfigSpec extends ZIOSpecDefault:

  def spec = suite("ConfigSpec")(
    test("Config Loaded") {
      for appConfig <- ZIO.service[AppConfig]
      yield assertTrue(appConfig.authConfig.private_key == "private_key")
    }
  ).provideLayer(config.TestConfig.appConfig)
