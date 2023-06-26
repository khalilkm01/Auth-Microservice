package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import models.enums.UserType
import models.persisted.Email
import repositories.EmailRepository
import zio.*

import java.sql.SQLException
import java.util.UUID

case class EmailRepositoryInMemory(state: Ref[Map[UUID, Email]]) extends InMemory[Email](state) with EmailRepository:

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
  override def getByEmailAddress(emailAddress: String, user: UserType): QIO[Email] =
    state.get.map(_.values.find(_.emailAddress == emailAddress).get)

  override def checkExistingId(id: UUID): QIO[Boolean] =
    state.get.map(_.contains(id))

  override def checkExistingEmailAddress(emailAddress: String, user: UserType): QIO[Boolean] =
    state.get.map(_.values.exists(_.emailAddress == emailAddress))

  override def connectEmail(id: UUID): QIO[Boolean] = ???

object EmailRepositoryInMemory:
  lazy val layer: ULayer[EmailRepository] =
    ZLayer.scoped {
      Ref.make(Map.empty[UUID, Email]).map(EmailRepositoryInMemory(_))
    }
