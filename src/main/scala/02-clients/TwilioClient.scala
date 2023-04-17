package clients

import models.enums.CountryCode
import zio.Task
trait TwilioClient:
  def requestPhoneCode(countryCode: CountryCode, digits: String): Task[Boolean]
  def verifyPhoneCode(countryCode: CountryCode, digits: String, code: String): Task[Boolean]
