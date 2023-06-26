package repositories.inmemory

import io.getquill.context.ZioJdbc.QIO
import models.enums.{ CountryCode, UserType }
import models.persisted.ContactNumber
import repositories.ContactNumberRepository
import zio.*

import java.sql.SQLException
import java.util.UUID

case class ContactNumberRepositoryInMemory(state: Ref[Map[UUID, ContactNumber]])
    extends InMemory[ContactNumber](state)
    with ContactNumberRepository:

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

  override def getByNumber(countryCode: CountryCode, digits: String, user: UserType): QIO[ContactNumber] =
    state.get.map(_.values.find(cn ⇒ cn.countryCode == countryCode && cn.digits == digits && cn.user == user).get)

  override def checkExistingId(id: UUID): QIO[Boolean] =
    state.get.map(_.contains(id))

  override def checkExistingNumber(countryCode: CountryCode, digits: String, user: UserType): QIO[Boolean] =
    state.get.map(_.values.exists(cn ⇒ cn.countryCode == countryCode && cn.digits == digits && cn.user == user))

  override def connectNumber(id: UUID): QIO[Boolean] =
    for
      cn <- getById(id)
      _ <- state
        .modify(m ⇒ (m + (id -> m(id).copy(connected = true)), m))
        .when(!cn.connected)
    yield true

  override def disconnectNumber(id: UUID): QIO[Boolean] =
    for
      cn <- getById(id)
      _ <- state
        .modify(m ⇒ (m + (id -> m(id).copy(connected = false)), m))
        .when(cn.connected)
    yield true

object ContactNumberRepositoryInMemory:

  lazy val layer: ULayer[ContactNumberRepository] =
    ZLayer.scoped {
      Ref.make(Map.empty[UUID, ContactNumber]).map(ContactNumberRepositoryInMemory(_))
    }
