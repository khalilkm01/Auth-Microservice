package config

import zio.{ Layer, TaskLayer }
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfig
import zio.config.{ ConfigDescriptor, ReadError }
import zio.config.ConfigDescriptor._
import zio.config.syntax.*

object Config:

  case class AppConfig(
    kafkaConfig: KafkaConfig,
    serverConfig: ServerConfig,
    authConfig: AuthConfig,
    twilioConfig: TwilioConfig,
    jdbc: DBConfig
  )

  case class ServerConfig(host: String, port: Int)

  case class KafkaConfig(
    server: String,
    groupId: String,
    clientId: String,
    parSize: Int,
    topics: List[String],
    publishTopic: String
  )

  case class AuthConfig(
    private_key: String
  )
  case class TwilioConfig(root: String, account_sid: String, auth_token: String, service_sid: String)

  case class DBConfig(url: String, driver: String, user: String, password: String)

  type AllConfig = AppConfig with KafkaConfig with ServerConfig with AuthConfig with TwilioConfig with DBConfig

  private final val Root = "application-conf"
  private final val Descriptor: ConfigDescriptor[AppConfig] =
    descriptor[AppConfig]

  val appConfig: Layer[ReadError[String], AppConfig] =
    TypesafeConfig.fromResourcePath(nested(Root)(Descriptor))

  val layer: TaskLayer[AllConfig] =
    appConfig >+>
      appConfig.narrow(_.serverConfig) >+>
      appConfig.narrow(_.authConfig) >+>
      appConfig.narrow(_.twilioConfig) >+>
      appConfig.narrow(_.kafkaConfig) >+>
      appConfig.narrow(_.jdbc)
