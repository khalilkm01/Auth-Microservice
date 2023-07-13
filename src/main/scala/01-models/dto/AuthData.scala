package models.dto

import models.enums.UserType

import zio.json._
import java.util.UUID
import org.joda.time.DateTime

case class AuthData(
  loginId: UUID,
  token: String,
  tokenExpiration: DateTime,
  authLevel: UserType
)

object AuthData:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given

  given JsonCodec[AuthData] = deriveCodec
