package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import models.persisted.Login
import repositories.LoginRepository
import zio.*

import java.sql.SQLException
import java.util.UUID

case class LoginRepositoryInMemory(state: Ref[Map[UUID, Login]]) extends LoginRepository:

  override def getById(id: UUID): QIO[Entity] =
    state.get.map(_(id))

  override def getById(ids: List[UUID]): QIO[List[Entity]] =
    state.get.map(_.view.filterKeys(ids.contains).values.toList)

  override def save(entity: Entity): QIO[Entity] =
    for
      exists <- state.get.map(_.contains(entity.id))
      _ <-
        if exists then state.update(_ + (entity.id -> entity))
        else ZIO.fail(SQLException(s"Model with id ${entity.id} does not exist"))
    yield entity

  override def create(entity: Entity): QIO[Entity] =
    for
      exists <- state.get.map(_.contains(entity.id))
      _ <-
        if !exists then state.update(_ + (entity.id -> entity))
        else ZIO.fail(SQLException(s"Model with existing id ${entity.id}"))
    yield entity

  override def delete(ids: List[UUID]): QIO[List[Entity]] =
    for {
      values <- getById(ids)
      _      <- state.update(_ -- ids)
    } yield values

  override def getAll: QIO[List[Entity]] =
    state.get.map(_.values.toList)

  override def getByEmailId(id: UUID): QIO[Login] =
    state.get.map(_.values.find(_.emailId == id).get)

  override def getByContactNumberId(id: UUID): QIO[Login] =
    state.get.map(_.values.find(_.contactNumberId == id).get)

  override def checkExistingId(id: UUID): QIO[Boolean] =
    state.get.map(_.contains(id))

  override def getByLoginId(id: UUID): QIO[Login] =
    getById(id)

object LoginRepositoryInMemory:
  lazy val layer: ULayer[LoginRepository] =
    ZLayer.scoped {
      Ref.make(Map.empty[UUID, Login]).map(LoginRepositoryInMemory(_))
    }
