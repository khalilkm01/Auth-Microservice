package config

import Config._
import zio.{ Layer, TaskLayer, ZIO }
import zio.config.*
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfig
import zio.config.{ ConfigDescriptor, ReadError }
import zio.config.ConfigDescriptor.*
import zio.config.syntax.*

import java.io.File

object TestConfig:

  private final val Root = "test-conf"
  private final val Descriptor: ConfigDescriptor[AppConfig] =
    descriptor[AppConfig]

  val appConfig: Layer[ReadError[String], AppConfig] =
    TypesafeConfig.fromResourcePath(nested(Root)(Descriptor))

  val authConfig: TaskLayer[AuthConfig]     = appConfig.narrow(_.authConfig)
  val twilioConfig: TaskLayer[TwilioConfig] = appConfig.narrow(_.twilioConfig)
