package repositories

import io.getquill.{ EntityQuery, Quoted }
import io.getquill.context.ZioJdbc.QIO

import java.util.UUID

trait Repository[T]:

  type Entity = T

  def create(entity: T): QIO[T]

  def save(entity: T): QIO[T]

  def delete(ids: List[UUID]): QIO[List[T]]

  def getById(ids: List[UUID]): QIO[List[T]]

  def getById(id: UUID): QIO[T]

  def getAll: QIO[List[T]]
