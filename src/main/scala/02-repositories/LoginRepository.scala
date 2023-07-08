package repositories

import models.persisted.Login

import io.getquill.context.ZioJdbc.QIO
import java.util.UUID

trait LoginRepository extends Repository[Login]:
  def getByLoginId(id: UUID): QIO[Login]
  def getByEmailId(id: UUID): QIO[Login]
  def getByContactNumberId(id: UUID): QIO[Login]

