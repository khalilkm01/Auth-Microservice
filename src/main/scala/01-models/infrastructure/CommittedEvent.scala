package models.infrastructure

import models.infrastructure.Event
import org.joda.time.DateTime
import zio.json.JsonCodec

import java.util.UUID

case class CommittedEvent(key: UUID, id: UUID, createdAt: DateTime, event: String)
//Have the event fully transformed to json

object CommittedEvent:
  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[CommittedEvent] = deriveCodec
