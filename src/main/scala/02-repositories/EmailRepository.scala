package repositories

import models.persisted.Email
import io.getquill.context.ZioJdbc.QIO
import models.enums.UserType

import java.util.UUID

trait EmailRepository extends Repository[Email]:
  def getByEmailAddress(emailAddress: String, user: UserType): QIO[Email]
  def checkExistingId(id: UUID): QIO[Boolean]
  def checkExistingEmailAddress(emailAddress: String, user: UserType): QIO[Boolean]
  def connectEmail(id: UUID): QIO[Boolean]
