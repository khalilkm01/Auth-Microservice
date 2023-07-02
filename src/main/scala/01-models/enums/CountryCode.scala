package models.enums

import models.common.{ JsonHelper, ServerError }

import zio._
import zio.json.JsonCodec

enum CountryCode(num: String):
  case GB extends CountryCode("44")
  case NG extends CountryCode("234")
  case US extends CountryCode("1")

  override def toString: String = this.num

  def fromCountryCode: UIO[String] = ZIO.succeed(this.num)

object CountryCode:

  def fromNum(
    num: String
  ): CountryCode =
    num match
      case "44"  ⇒ CountryCode.GB
      case "1"   ⇒ CountryCode.US
      case "234" ⇒ CountryCode.NG
      case _     ⇒ throw IllegalArgumentException(ServerError.InternalServerErrorMessage.IllegalArgumentMessage)

  def fromString(
    num: String
  ): CountryCode =
    num match
      case "GB" ⇒ CountryCode.GB
      case "US" ⇒ CountryCode.US
      case "NG" ⇒ CountryCode.NG
      case _    ⇒ throw IllegalArgumentException(ServerError.InternalServerErrorMessage.IllegalArgumentMessage)

  def toCountryCode(num: String): IO[ServerError.EnumConversionError.CountryCodeConversionError, CountryCode] =
    num match
      case "GB" ⇒ ZIO.succeed(CountryCode.GB)
      case "US" ⇒ ZIO.succeed(CountryCode.US)
      case "NG" ⇒ ZIO.succeed(CountryCode.NG)
      case _    ⇒ ZIO.fail(ServerError.EnumConversionError.CountryCodeConversionError(num))

  given JsonCodec[CountryCode] =
    JsonHelper.deriveCodec
