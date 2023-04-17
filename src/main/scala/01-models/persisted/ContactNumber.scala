package models.persisted

import models.enums.{ CountryCode, UserType }
import zio.json.JsonCodec

import java.util.UUID
import org.joda.time.DateTime

case class ContactNumber(
  id: UUID,
  countryCode: CountryCode,
  digits: String,
  connected: Boolean,
  user: UserType,
  createdAt: DateTime,
  updatedAt: DateTime
)

object ContactNumber:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[ContactNumber] = deriveCodec
