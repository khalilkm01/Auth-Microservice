package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import repositories.Repository
import zio.Ref

import java.util.UUID

trait InMemory[Entity](state: Ref[Map[UUID, Entity]]) extends Repository[Entity]:
  override def getById(id: UUID): QIO[Entity] =
    state.get.map(_(id))

  override def getById(ids: List[UUID]): QIO[List[Entity]] =
    state.get.map(_.view.filterKeys(ids.contains).values.toList)

  override def delete(ids: List[UUID]): QIO[List[Entity]] =
    for {
      values <- getById(ids)
      _      <- state.update(_ -- ids)
    } yield values

  override def getAll: QIO[List[Entity]] =
    state.get.map(_.values.toList)
