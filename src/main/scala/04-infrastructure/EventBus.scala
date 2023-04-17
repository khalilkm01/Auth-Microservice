package infrastructure

import zio.Task
import zio.json.JsonDecoder
import zio.kafka.consumer.Subscription

trait EventBus:
  def start(
      parSize: Int,
      topics: Subscription
  ): Task[Unit]
