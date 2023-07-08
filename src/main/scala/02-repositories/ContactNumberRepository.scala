package repositories

import models.enums.{ CountryCode, UserType }
import models.persisted.ContactNumber
import io.getquill.context.ZioJdbc.QIO

import java.util.UUID

trait ContactNumberRepository extends Repository[ContactNumber]:

  def getByNumber(countryCode: CountryCode, digits: String, user: UserType): QIO[ContactNumber]

  def checkExistingNumber(countryCode: CountryCode, digits: String, user: UserType): QIO[Boolean]

  def connectNumber(id: UUID): QIO[Boolean]

  def disconnectNumber(id: UUID): QIO[Boolean]

//  def updateName(id: UUID, name: String): QIO[ContactNumberType]
