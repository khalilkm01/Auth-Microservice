package models.persisted

import models.enums.UserType
import zio.json.JsonCodec
import org.joda.time.DateTime

import java.util.UUID

case class Login(
  id: UUID,
  password: String,
  blocked: Boolean,
  userType: UserType,
  emailId: UUID,
  contactNumberId: UUID,
  createdAt: DateTime,
  updatedAt: DateTime
)

object Login:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[Login] = deriveCodec
