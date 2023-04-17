package gateway

import zio.RIO

trait Gateway[ServicesLayer]:
  type Services = ServicesLayer

  val startingMessage: String

  def start: RIO[Services, Unit]

object Gateway
