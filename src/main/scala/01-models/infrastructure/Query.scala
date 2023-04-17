package models.infrastructure

import models.common.Auth
import models.enums.{ CountryCode, UserType }

import zio.json.JsonCodec

import java.util.UUID

sealed trait Query
case class IsAuthQuery(token: String)                               extends Query
case class AuthByLevelQuery(auth: Auth, authLevels: List[UserType]) extends Query
case class AuthByIdQuery(auth: Auth, id: UUID)                      extends Query

case class RequestPhoneCodeQuery(countryCode: CountryCode, digits: String)                    extends Query
case class SetupContactNumberQuery(countryCode: CountryCode, digits: String, user: UserType)  extends Query
case class UpdateContactNumberQuery(countryCode: CountryCode, digits: String, user: UserType) extends Query

case class LoginUserQuery(emailAddress: String, password: String, user: UserType) extends Query
case class CreateLoginQuery(
  emailAddress: String,
  password: String,
  countryCode: String,
  digits: String,
  code: String,
  user: UserType
) extends Query
case class UpdatePasswordQuery(id: UUID, loginId: UUID, currentPassword: String, newPassword: String) extends Query

case class CheckEmailAvailableQuery(emailAddress: String, user: UserType) extends Query

object Query:
  import models.common.JsonHelper.deriveCodec
  import Auth.given
  import UserType.given

  given JsonCodec[Query]                    = deriveCodec
  given JsonCodec[IsAuthQuery]              = deriveCodec
  given JsonCodec[AuthByLevelQuery]         = deriveCodec
  given JsonCodec[AuthByIdQuery]            = deriveCodec
  given JsonCodec[RequestPhoneCodeQuery]    = deriveCodec
  given JsonCodec[SetupContactNumberQuery]  = deriveCodec
  given JsonCodec[LoginUserQuery]           = deriveCodec
  given JsonCodec[CreateLoginQuery]         = deriveCodec
  given JsonCodec[UpdatePasswordQuery]      = deriveCodec
  given JsonCodec[CheckEmailAvailableQuery] = deriveCodec
