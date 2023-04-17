package models.common

import models.enums.UserType

import zio.json._
import java.util.UUID

case class Auth(loginId: UUID, isAuth: Boolean, authLevel: UserType)

object Auth:
  import models.common.JsonHelper
  import JsonHelper.{ deriveCodec, given }

  given JsonCodec[Auth] = deriveCodec
