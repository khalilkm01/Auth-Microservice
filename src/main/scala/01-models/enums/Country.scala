package models.enums

import models.common.{ JsonHelper, ServerError }

import zio.{ IO, Task, UIO, ZIO }
import zio.json.JsonCodec

enum Country(country: String):
  case GB extends Country("GB")
  case US extends Country("US")
  case NG extends Country("NG")

  def fromCountry: UIO[String] = ZIO.succeed(this.country)

object Country:
  def toCountry(country: String): IO[ServerError.EnumConversionError.CountryConversionError, Country] =
    country match
      case "GB" ⇒ ZIO.succeed(Country.GB)
      case "US" ⇒ ZIO.succeed(Country.US)
      case "NG" ⇒ ZIO.succeed(Country.NG)
      case _    ⇒ ZIO.fail(ServerError.EnumConversionError.CountryConversionError(country))

  given JsonCodec[Country] = JsonHelper.deriveCodec
