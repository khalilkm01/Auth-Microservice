import clients.implementation.{ JwtClientLive, TwilioClientLive }
import clients.{ JwtClient, TwilioClient }
import infrastructure.EventBus
import infrastructure.EventHandler
import infrastructure.handler.EventHandlerLive
import infrastructure.kafka.EventBusLive
import config.Config
import gateway.Gateway
import gateway.implementation.ApiGateway
import services.{ AuthService, ContactNumberService, EmailService, LoginService }
import services.implementation.{ AuthServiceLive, ContactNumberServiceLive, EmailServiceLive, LoginServiceLive }
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import repositories.quill.{ ContactNumberRepositoryLive, EmailRepositoryLive, LoginRepositoryLive }

import zio.*
import zio.kafka.consumer.{ Consumer, ConsumerSettings, Subscription }
import zio.Console.{ printLine, readLine }
import zio.http.{ Server, ServerConfig }
//import zio.logging.LogFormat
//import zio.logging.backend.SLF4J
import zio.logging.slf4j.bridge.Slf4jBridge

import java.io.IOException
import javax.sql.DataSource

object Program:

  private val kafkaConfiguration: TaskLayer[Config.KafkaConsumerConfig] =
    ZLayer.scoped {
      for {
        kafkaConfig <- ZIO
          .serviceWith[Config.AppConfig](_.kafkaConsumerConfig)
          .provide(Config.layer)
          .orDie
      } yield kafkaConfig
    }

  private lazy val serverConfiguration: TaskLayer[ServerConfig] =
    ZLayer
      .scoped {
        for {
          conf <- ZIO
            .serviceWith[Config.AppConfig](_.serverConfig)
            .provide(Config.layer)
            .orDie
          host = conf.host
          port = conf.port
        } yield ServerConfig.default.binding(host, port)
      }

  private lazy val authConfiguration: TaskLayer[Config.AuthConfig] =
    ZLayer
      .scoped {
        for {
          authConfig <- ZIO
            .serviceWith[Config.AppConfig](_.authConfig)
            .provide(Config.layer)
            .orDie
        } yield authConfig
      }

  private lazy val twilioConfiguration: TaskLayer[Config.TwilioConfig] =
    ZLayer
      .scoped {
        for {
          twilioConfig <- ZIO
            .serviceWith[Config.AppConfig](_.twilioConfig)
            .provide(Config.layer)
            .orDie
        } yield twilioConfig
      }

  private lazy val dbConfiguration: TaskLayer[Config.DBConfig] =
    ZLayer
      .scoped {
        for {
          dbConfig <- ZIO
            .serviceWith[Config.AppConfig](_.jdbc)
            .provide(Config.layer)
            .orDie
        } yield dbConfig
      }

  private lazy val clientsLayer: TaskLayer[JwtClient with TwilioClient] =
    ZLayer.make[JwtClient with TwilioClient](
      JwtClientLive.layer,
      TwilioClientLive.layer,
      authConfiguration,
      twilioConfiguration
    )

  private lazy val repositoriesLayer: TaskLayer[ContactNumberRepository with EmailRepository with LoginRepository] =
    ZLayer.make[ContactNumberRepository with EmailRepository with LoginRepository](
      ContactNumberRepositoryLive.layer,
      EmailRepositoryLive.layer,
      LoginRepositoryLive.layer
    )

  private lazy val servicesLayer: TaskLayer[ApiGateway.Services] =
    ZLayer.make[ApiGateway.Services](
      AuthServiceLive.layer,
      ContactNumberServiceLive.layer,
      EmailServiceLive.layer,
      LoginServiceLive.layer,
      authConfiguration,
      repositoriesLayer,
      clientsLayer
    )

  private lazy val serverLayer: TaskLayer[Server] =
    ZLayer.make[Server](
      Server.live,
      serverConfiguration
    )

  private lazy val gatewayLayer: TaskLayer[ApiGateway with ApiGateway.Services with Server] =
    ZLayer.make[ApiGateway with ApiGateway.Services with Server](
      ApiGateway.layer,
      serverLayer,
      servicesLayer
    )

  private lazy val setupDB: Task[Unit] = {
    for
      dbConfig <- ZIO.service[Config.DBConfig]
      _        <- FlywayMigration.migrate(dbConfig)
    yield ()
  }.provide(dbConfiguration)

  private lazy val gateway: Task[Unit] =
    ZIO.serviceWithZIO[ApiGateway](_.start).provide(gatewayLayer)

  private val logger: ULayer[Unit] =
    Slf4jBridge.initialize

  lazy val make: ZIO[Scope, Any, Any] =
    setupDB *> gateway
      .provide(logger)

//  private val consumerLayer: TaskLayer[Consumer] =
//    ZLayer.scoped(
//      for {
//        kafkaConfig <- ZIO
//          .serviceWith[Config.AppConfig](_.kafkaConsumerConfig)
//          .provide(Config.layer)
//        consumerSettings = ConsumerSettings(List(kafkaConfig.server))
//          .withGroupId(kafkaConfig.groupId)
//          .withCloseTimeout(30.seconds)
//        consumer <- Consumer.make(consumerSettings).orDie
//      } yield consumer
//    )
//
//  private lazy val eventBusLayer: TaskLayer[EventBus] =
//    ZLayer.make[EventBus](
//      EventBusLive.layer,
//      consumerLayer,
//      EventHandlerLive.layer,
//      servicesLayer
//    )
//  private val eventBus: RIO[EventBusLayer, Unit] =
//    for {
//      kafkaConfig <- ZIO.service[Config.KafkaConsumerConfig]
//      topics = Subscription.Topics(kafkaConfig.topics.toSet)
//      _ <- ZIO.log("Starting Event Bus") *> ZIO
//        .serviceWithZIO[EventBus](
//          _.start(
//            kafkaConfig.parSize,
//            topics
//          )
//        )
//    } yield ()
//
