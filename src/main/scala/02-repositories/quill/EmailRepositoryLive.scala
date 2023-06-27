package repositories.quill

import models.common.ServerError
import models.persisted.Email
import repositories.EmailRepository
import io.getquill.*
import io.getquill.context.ZioJdbc.QIO
import io.getquill.jdbczio.Quill
import models.enums.UserType
import zio.*
import org.joda.time.DateTime

import java.sql.SQLException
import java.util.UUID

final case class EmailRepositoryLive() extends EmailRepository:
  import QuillContext.{ *, given }

  private type Entity = Email

  override def getById(id: UUID): QIO[Email] =
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

  override def getById(ids: List[UUID]): QIO[List[Email]] =
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

  override def create(entity: Email): QIO[Email] =
    run {
      quote {
        query[Entity]
          .insert(
            _.emailAddress -> lift(entity.emailAddress),
            _.verified     -> lift(entity.verified),
            _.connected    -> lift(entity.connected),
            _.userType     -> lift(entity.userType)
          )
          .returning(entity ⇒ entity)
      }
    }

  override def delete(ids: List[UUID]): QIO[List[Email]] =
    run(
      quote {
        query[Entity]
          .filter(entity ⇒ liftQuery(ids).contains(entity.id))
          .delete
          .returningMany(entity ⇒ entity)
      }
    )

  override def save(entity: Email): QIO[Email] =
    run {
      quote {
        query[Entity]
          .filter(obj ⇒ obj.id == lift(entity.id))
          .updateValue(lift(entity))
          .returning(entity ⇒ entity)
      }
    }

  override def getByEmailAddress(emailAddress: String, user: UserType): QIO[Email] =
    run(
      quote {
        query[Entity].filter(email ⇒ lift(emailAddress) == email.emailAddress && lift(user) == email.userType)
      }
    ).map {
      _.headOption match
        case None        ⇒ throw SQLException("EMAIL NOT FOUND")
        case Some(email) ⇒ email
    }

  override def checkExistingId(id: UUID): QIO[Boolean] =
    run(quote {
      query[Entity].filter(email ⇒ lift(id) == email.id)
    }).map {
      _.headOption match
        case None    ⇒ false
        case Some(_) ⇒ true
    }

  override def checkExistingEmailAddress(emailAddress: String, user: UserType): QIO[Boolean] =
    run(quote {
      query[Entity].filter(email ⇒ lift(emailAddress) == email.emailAddress && lift(user) == email.userType)
    }).map {
      _.headOption match
        case None    ⇒ false
        case Some(_) ⇒ true
    }

  override def connectEmail(id: UUID): QIO[Boolean] =
    getById(id).foldZIO(
      ZIO.fail(_),
      {
        case email if !email.connected ⇒
          save(
            Email(
              id = email.id,
              emailAddress = email.emailAddress,
              verified = false,
              connected = true,
              userType = email.userType,
              createdAt = email.createdAt,
              updatedAt = DateTime.now
            )
          ).as(true)
        case _ ⇒ ZIO.succeed(false)
      }
    )

object EmailRepositoryLive:
  lazy val layer: TaskLayer[EmailRepository] =
    ZLayer(ZIO.attempt(new EmailRepositoryLive))
