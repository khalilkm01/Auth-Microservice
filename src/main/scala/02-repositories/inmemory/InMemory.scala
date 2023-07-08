package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import repositories.Repository
import zio.Ref

import java.sql.SQLException
import java.util.UUID

trait InMemory[Entity](state: Ref[Map[UUID, Entity]]) extends Repository[Entity]:

  override def getById(id: UUID): QIO[Entity] =
    state.get.map(_.getOrElse(id, throw new SQLException(s"Entity with id $id not found")))

  override def getById(ids: List[UUID]): QIO[List[Entity]] =
    state.get.map(_.view.filterKeys(ids.contains).values.toList)

  override def delete(ids: List[UUID]): QIO[List[Entity]] =
    for {
      values <- getById(ids)
      _      <- state.update(_ -- ids)
    } yield values

  override def getAll: QIO[List[Entity]] =
    state.get.map(_.values.toList)

  override def checkExistingId(id: UUID): QIO[Boolean] =
    state.get.map(_.contains(id))
