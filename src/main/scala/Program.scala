import config.Config
import clients.implementation.{ JwtClientLive, TwilioClientLive }
import clients.{ JwtClient, TwilioClient }
import publisher.kafka.EventPublisherLive
import repositories.{ ContactNumberRepository, EmailRepository, LoginRepository }
import repositories.quill.{ ContactNumberRepositoryLive, EmailRepositoryLive, LoginRepositoryLive }
import services.{ AuthService, ContactNumberService, EmailService, LoginService }
import services.implementation.{ AuthServiceLive, ContactNumberServiceLive, EmailServiceLive, LoginServiceLive }
import gateway.Gateway
import gateway.api.ApiGateway
import gateway.infrastructure.{ EventHandler, KafkaGateway }
import gateway.infrastructure.implementation.EventHandlerLive
import publisher.EventPublisher
import zio.*
import zio.http.{ Server, ServerConfig }
import zio.kafka.consumer.{ Consumer, ConsumerSettings, Subscription }
import zio.kafka.producer.{ Producer, ProducerSettings }
//import zio.logging.LogFormat
//import zio.logging.backend.SLF4J
import zio.logging.slf4j.bridge.Slf4jBridge

import java.io.IOException
import javax.sql.DataSource

object Program:

  private lazy val serverConfiguration: TaskLayer[ServerConfig] =
    ZLayer
      .scoped {
        for
          conf <- ZIO
            .serviceWith[Config.AppConfig](_.serverConfig)
            .provide(Config.layer)
            .orDie
          host = conf.host
          port = conf.port
        yield ServerConfig.default.binding(host, port)
      }

  private lazy val authConfiguration: TaskLayer[Config.AuthConfig] =
    ZLayer
      .scoped {
        for authConfig <- ZIO
            .serviceWith[Config.AppConfig](_.authConfig)
            .provide(Config.layer)
            .orDie
        yield authConfig
      }

  private lazy val twilioConfiguration: TaskLayer[Config.TwilioConfig] =
    ZLayer
      .scoped {
        for twilioConfig <- ZIO
            .serviceWith[Config.AppConfig](_.twilioConfig)
            .provide(Config.layer)
            .orDie
        yield twilioConfig
      }

  private lazy val dbConfiguration: TaskLayer[Config.DBConfig] =
    ZLayer
      .scoped {
        for dbConfig <- ZIO
            .serviceWith[Config.AppConfig](_.jdbc)
            .provide(Config.layer)
            .orDie
        yield dbConfig
      }

  private lazy val kafkaConfiguration: TaskLayer[Config.KafkaConfig] =
    ZLayer
      .scoped {
        for kafkaConfig <- ZIO
            .serviceWith[Config.AppConfig](_.kafkaConfig)
            .provide(Config.layer)
            .orDie
        yield kafkaConfig
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

  private val consumerLayer: TaskLayer[Consumer] =
    ZLayer.scoped(
      for
        kafkaConfig <- ZIO
          .service[Config.KafkaConfig]
          .provide(kafkaConfiguration)
        consumerSettings = ConsumerSettings(List(kafkaConfig.server))
          .withGroupId(kafkaConfig.groupId)
          .withCloseTimeout(30.seconds)
        consumer <- Consumer.make(consumerSettings).orDie
      yield consumer
    )

  private val producerLayer: TaskLayer[Producer] =
    ZLayer.scoped(
      for
        kafkaConfig <- ZIO
          .service[Config.KafkaConfig]
          .provide(kafkaConfiguration)
        producerSettings = ProducerSettings(List(kafkaConfig.server))
          .withClientId(kafkaConfig.clientId)
          .withCloseTimeout(30.seconds)
        producer <- Producer.make(producerSettings).orDie
      yield producer
    )

  private val subscriptionLayer: TaskLayer[Subscription] =
    ZLayer.scoped(
      for
        kafkaConfig <- ZIO
          .service[Config.KafkaConfig]
          .provide(kafkaConfiguration)
        subscription <- ZIO.succeed(Subscription.topics(kafkaConfig.topics.head, kafkaConfig.topics.tail: _*))
      yield subscription
    )

  private val kafkaLayers: TaskLayer[Consumer with Producer with Subscription] =
    consumerLayer ++ producerLayer ++ subscriptionLayer

  private lazy val servicesLayer: TaskLayer[ApiGateway.Services] =
    ZLayer.make[ApiGateway.Services](
      AuthServiceLive.layer,
      ContactNumberServiceLive.layer,
      EmailServiceLive.layer,
      LoginServiceLive.layer,
      EventPublisherLive.layer,
      producerLayer,
      authConfiguration,
      repositoriesLayer,
      clientsLayer
    )

  private lazy val serverLayer: TaskLayer[Server] =
    ZLayer.make[Server](
      Server.live,
      serverConfiguration
    )

  private lazy val kafkaGatewayLayer: TaskLayer[KafkaGateway with KafkaGateway.EnvironmentIn] =
    ZLayer.make[KafkaGateway with KafkaGateway.EnvironmentIn](
      KafkaGateway.layer,
      EventHandlerLive.layer,
      EventPublisherLive.layer,
      servicesLayer,
      kafkaLayers
    )

  private lazy val ApiGatewayLayer: TaskLayer[ApiGateway with ApiGateway.EnvironmentIn] =
    ZLayer.make[ApiGateway with ApiGateway.EnvironmentIn](
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

  private lazy val ApiGatewayStart: Task[Unit] =
    ZIO.serviceWithZIO[ApiGateway](_.start).provide(ApiGatewayLayer)

  private lazy val KafkaGatewayStart: Task[Unit] =
    ZIO.serviceWithZIO[KafkaGateway](_.start).provide(kafkaGatewayLayer)

  private val logger: ULayer[Unit] =
    Slf4jBridge.initialize

  lazy val make: ZIO[Scope, Any, Any] =
    setupDB *> ApiGatewayStart
//      <&> KafkaGatewayStart
      .provide(logger)
