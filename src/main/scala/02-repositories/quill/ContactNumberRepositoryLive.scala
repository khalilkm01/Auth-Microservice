package repositories.quill

import models.common.ServerError
import models.persisted.ContactNumber
import models.enums.{ CountryCode, UserType }
import repositories.ContactNumberRepository

import io.getquill.*
import io.getquill.context.ZioJdbc.QIO
import io.getquill.jdbczio.Quill
import zio.*
import org.joda.time.DateTime

import java.sql.SQLException
import java.util.UUID

final case class ContactNumberRepositoryLive() extends ContactNumberRepository:
  import QuillContext.{ *, given }

  private type Entity = ContactNumber

  override def getById(id: UUID): QIO[ContactNumber] =
    for {
      entityList <- run {
        quote {
          query[Entity].filter(entity ⇒ lift(id) == entity.id)
        }
      }
      entity <- ZIO
        .fromOption(entityList.headOption)
        .orElseFail(SQLException(ServerError.SQLExceptionMessage.IDNotFound(id)))
    } yield entity

  override def getById(ids: List[UUID]): QIO[List[ContactNumber]] =
    run {
      quote {
        query[Entity].filter(entity ⇒ liftQuery(ids).contains(entity.id))
      }
    }

  override def getAll: QIO[List[Entity]] =
    run {
      quote {
        query[Entity]
      }
    }

  override def create(entity: ContactNumber): QIO[ContactNumber] =
    run {
      quote {
        query[Entity]
          .insert(
            _.countryCode -> lift(entity.countryCode),
            _.digits      -> lift(entity.digits),
            _.connected   -> lift(entity.connected),
            _.userType    -> lift(entity.userType)
          )
          .returning(entity ⇒ entity)
      }
    }

  override def delete(ids: List[UUID]): QIO[List[ContactNumber]] =
    run(
      quote {
        query[Entity]
          .filter(entity ⇒ liftQuery(ids).contains(entity.id))
          .delete
          .returningMany(entity ⇒ entity)
      }
    )

  override def save(entity: ContactNumber): QIO[ContactNumber] =
    run {
      quote {
        query[Entity]
          .filter(obj ⇒ obj.id == lift(entity.id))
          .updateValue(lift(entity))
          .returning(entity ⇒ entity)
      }
    }

  override def checkExistingId(id: UUID): QIO[Boolean] = run(quote {
    query[Entity].filter(contactNumber ⇒ lift(id) == contactNumber.id)
  }).map {
    _.headOption match
      case None                ⇒ false
      case Some(contactNumber) ⇒ true
  }

  override def getByNumber(countryCode: CountryCode, digits: String, user: UserType): QIO[ContactNumber] =
    for {
      entityList <- run(
        quote {
          query[Entity].filter(contactNumber ⇒
            lift(countryCode) == contactNumber.countryCode
              && lift(digits) == contactNumber.digits
              && lift(user) == contactNumber.userType
          )
        }
      )
      entity <- ZIO
        .fromOption(entityList.headOption)
        .orElseFail(SQLException(ServerError.SQLExceptionMessage.EntityDoesNotExist("ContactNumber")))
    } yield entity

  override def checkExistingNumber(
    countryCode: CountryCode,
    digits: String,
    user: UserType
  ): QIO[Boolean] =
    getByNumber(
      countryCode = countryCode,
      digits = digits,
      user = user
    ).fold(_ ⇒ false, _ ⇒ true)

  override def connectNumber(id: UUID): QIO[Boolean] =
    getById(id).foldZIO(
      ZIO.fail(_),
      {
        case contactNumber if !contactNumber.connected ⇒
          save(
            ContactNumber(
              contactNumber.id,
              contactNumber.countryCode,
              contactNumber.digits,
              true,
              contactNumber.userType,
              contactNumber.createdAt,
              DateTime.now
            )
          ).as(true)
        case _ ⇒ ZIO.succeed(false)
      }
    )

  override def disconnectNumber(id: UUID): QIO[Boolean] =
    getById(id).foldZIO(
      ZIO.fail(_),
      {
        case contactNumber if contactNumber.connected ⇒
          save(
            ContactNumber(
              contactNumber.id,
              contactNumber.countryCode,
              contactNumber.digits,
              false,
              contactNumber.userType,
              contactNumber.createdAt,
              DateTime.now
            )
          ).as(true)
        case _ ⇒ ZIO.succeed(false)
      }
    )

object ContactNumberRepositoryLive:
  lazy val layer: TaskLayer[ContactNumberRepository] =
    ZLayer(ZIO.attempt(new ContactNumberRepositoryLive))
