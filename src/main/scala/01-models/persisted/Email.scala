package models.persisted

import models.enums.UserType

import java.util.UUID
import org.joda.time.DateTime
import zio.json.JsonCodec

case class Email(
                  id: UUID,
                  emailAddress: String,
                  verified: Boolean,
                  userType: UserType,
                  connected: Boolean,
                  createdAt: DateTime,
                  updatedAt: DateTime
)

object Email:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[Email] = deriveCodec
