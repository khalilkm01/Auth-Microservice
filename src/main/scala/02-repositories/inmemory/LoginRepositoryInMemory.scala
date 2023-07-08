package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import models.persisted.Login
import repositories.LoginRepository
import zio.*

import java.sql.SQLException
import java.util.UUID

case class LoginRepositoryInMemory(state: Ref[Map[UUID, Login]]) extends InMemory[Login](state) with LoginRepository:

  override def save(entity: Entity): QIO[Entity] =
    for
      exists <- state.get.map(_.contains(entity.id))
      _ <-
        if exists then state.update(_ + (entity.id -> entity))
        else ZIO.fail(SQLException(s"Model with id ${entity.id} does not exist"))
      newEntity <- getById(entity.id)
    yield newEntity

  override def create(entity: Entity): QIO[Entity] =
    for
      newEntity <- ZIO.succeed(entity.copy(id = UUID.randomUUID()))
      _         <- state.update(_ + (newEntity.id -> newEntity))
    yield newEntity

  override def getByEmailId(id: UUID): QIO[Login] =
    state.get.map(_.values.find(_.emailId == id).get)

  override def getByContactNumberId(id: UUID): QIO[Login] =
    state.get.map(_.values.find(_.contactNumberId == id).get)

  override def getByLoginId(id: UUID): QIO[Login] =
    getById(id)

object LoginRepositoryInMemory:
  lazy val layer: ULayer[LoginRepository] =
    ZLayer.scoped {
      Ref.make(Map.empty[UUID, Login]).map(LoginRepositoryInMemory(_))
    }
