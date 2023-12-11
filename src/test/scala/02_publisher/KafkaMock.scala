package publisher

import config.{ Config, TestConfig }
import zio.*
import zio.kafka.consumer.{ Consumer, ConsumerSettings, Subscription }
import zio.kafka.producer.{ Producer, ProducerSettings }
import io.github.scottweaver.zio.testcontainers.kafka.ZKafkaContainer
import com.dimafeng.testcontainers.KafkaContainer
import org.testcontainers.utility.DockerImageName

object KafkaMock:

  private val dockerImageName = s"${KafkaContainer.defaultImage}:7.4.1"

  private val kafkaContainer: TaskLayer[KafkaContainer.Def] = ZLayer
    .succeed(KafkaContainer.Def(DockerImageName.parse(dockerImageName)))

  val kafkaContainerLayer: TaskLayer[KafkaContainer] =
    ZKafkaContainer.Settings.default >+> ZKafkaContainer.live

  lazy val consumerLayer: TaskLayer[Consumer] =
    ZLayer.scoped(
      for
        consumerSettings <- ZIO
          .service[ConsumerSettings]
          .provide(kafkaContainerLayer >+> ZKafkaContainer.defaultConsumerSettings)
        consumer <- Consumer.make(consumerSettings).orDie
      yield consumer
    )

  lazy val producerLayer: TaskLayer[Producer] =
    ZLayer.scoped(
      for
        producerSettings <- ZIO
          .service[ProducerSettings]
          .provide(kafkaContainerLayer >+> ZKafkaContainer.defaultProducerSettings)
        producer <- Producer.make(producerSettings).orDie
      yield producer
    )

  lazy val subscriptionLayer: TaskLayer[Subscription] =
    ZLayer.scoped(
      for
        kafkaConfig <- ZIO
          .service[Config.KafkaConfig]
          .provide(TestConfig.kafkaConfig)
        subscription <- ZIO.succeed(Subscription.topics(kafkaConfig.topics.head, kafkaConfig.topics.tail: _*))
      yield subscription
    )
