package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import models.enums.UserType
import models.persisted.Email
import org.joda.time.DateTime
import repositories.EmailRepository
import zio.*

import java.sql.SQLException
import java.util.UUID

case class EmailRepositoryInMemory(state: Ref[Map[UUID, Email]]) extends InMemory[Email](state) with EmailRepository:

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
      newEntity <- ZIO.succeed(entity.copy(id = UUID.randomUUID))
      _         <- state.update(_ + (newEntity.id -> newEntity))
    yield newEntity

  override def getByEmailAddress(emailAddress: String, user: UserType): QIO[Email] =
    state.get.map(_.values.find(_.emailAddress == emailAddress).get)

  override def checkExistingEmailAddress(emailAddress: String, user: UserType): QIO[Boolean] =
    state.get.map(_.values.exists(_.emailAddress == emailAddress))

  override def connectEmail(id: UUID): QIO[Boolean] =
    for
      e <- getById(id)
      _ <- state
        .updateAndGet(_ + (id -> e.copy(connected = true)))
        .when(!e.connected)
      emailConnected <- getById(id).map(_.connected)
    yield emailConnected

object EmailRepositoryInMemory:
  lazy val layer: ULayer[EmailRepository] =
    ZLayer.scoped {
      Ref.make(Map.empty[UUID, Email]).map(EmailRepositoryInMemory(_))
    }
