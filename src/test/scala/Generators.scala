package testUtils

import models.persisted.*
import models.enums.*
import org.joda.time.DateTime
import zio.Random
import zio.test.*

import java.util.UUID

object Generators:

  val UUIDGen: Gen[Random, UUID] = Gen.uuid

  val nonEmptyStringGen: Gen[Random, String] = Gen.alphaNumericStringBounded(21, 40)

  val userTypeGen: Gen[Random, UserType] = Gen.int(1, 3).map {
    case 0 => UserType.ADMIN
    case 1 => UserType.CUSTOMER
    case 2 => UserType.DOCTOR
  }

  val countryCodeGen: Gen[Random, CountryCode] = Gen.int(1, 3).map {
    case 0 => CountryCode.GB
    case 1 => CountryCode.NG
    case 2 => CountryCode.US
  }

  def loginGen(blocked: Boolean): Gen[Random, Login] =
    for
      id              <- UUIDGen
      emailId         <- UUIDGen
      contactNumberId <- UUIDGen
      password        <- nonEmptyStringGen
      userType        <- userTypeGen
      now = DateTime.now()
    yield Login(
      id = id,
      password = password,
      blocked = blocked,
      userType = userType,
      emailId = emailId,
      contactNumberId = contactNumberId,
      createdAt = now,
      updatedAt = now
    )
