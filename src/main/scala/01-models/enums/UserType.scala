package models.enums

import models.common.ServerError
import zio.{ IO, UIO, ZIO }
import zio.json.{ DeriveJsonCodec, JsonCodec }

enum UserType(user: String):
  case ADMIN    extends UserType("ADMIN")
  case CUSTOMER extends UserType("CUSTOMER")
  case DOCTOR   extends UserType("DOCTOR")

  def fromUserType: UIO[String] = ZIO.succeed(this.user)

object UserType:
  def fromString(
    user: String
  ): UserType =
    user match
      case "ADMIN"    ⇒ UserType.ADMIN
      case "CUSTOMER" ⇒ UserType.CUSTOMER
      case "DOCTOR"   ⇒ UserType.DOCTOR
      case _          ⇒ throw IllegalArgumentException(ServerError.InternalServerErrorMessage.IllegalArgumentMessage)

  def toUserType(
    user: String
  ): IO[ServerError.EnumConversionError.UserTypeConversionError, UserType] =
    user match
      case "ADMIN"    ⇒ ZIO.succeed(UserType.ADMIN)
      case "CUSTOMER" ⇒ ZIO.succeed(UserType.CUSTOMER)
      case "DOCTOR"   ⇒ ZIO.succeed(UserType.DOCTOR)
      case _          ⇒ ZIO.fail(ServerError.EnumConversionError.UserTypeConversionError(user))

  import models.common.JsonHelper
  import JsonHelper.{ deriveCodec, given }

  given JsonCodec[UserType] = deriveCodec
