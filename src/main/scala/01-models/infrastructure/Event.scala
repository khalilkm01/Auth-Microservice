package models.infrastructure

import zio.json.JsonCodec

import java.util.UUID

sealed trait Event
case class DefaultEvent() extends Event

object Event:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[Event] = deriveCodec
