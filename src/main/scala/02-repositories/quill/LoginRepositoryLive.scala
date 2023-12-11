package repositories.quill

import models.common.ServerError
import models.persisted.Login
import repositories.LoginRepository

import io.getquill.*
import io.getquill.context.ZioJdbc.QIO
import io.getquill.jdbczio.Quill
import zio.*
import org.joda.time.DateTime

import java.sql.SQLException
import java.util.UUID

final case class LoginRepositoryLive() extends LoginRepository:
  import QuillContext.{ *, given }

  private type Entity = Login

  override def getById(id: UUID): QIO[Login] =
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

  override def getById(ids: List[UUID]): QIO[List[Login]] =
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

  override def create(entity: Login): QIO[Login] =
    run {
      quote {
        query[Entity]
          .insert(
            _.id              -> lift(entity.id),
            _.password        -> lift(entity.password),
            _.blocked         -> lift(entity.blocked),
            _.userType        -> lift(entity.userType),
            _.emailId         -> lift(entity.emailId),
            _.contactNumberId -> lift(entity.contactNumberId)
          )
          .returning(entity ⇒ entity)
      }
    }

  override def delete(ids: List[UUID]): QIO[List[Login]] =
    run(
      quote {
        query[Entity]
          .filter(entity ⇒ liftQuery(ids).contains(entity.id))
          .delete
          .returningMany(entity ⇒ entity)
      }
    )

  override def save(entity: Login): QIO[Login] =
    run {
      quote {
        query[Entity]
          .filter(obj ⇒ obj.id == lift(entity.id))
          .updateValue(lift(entity))
          .returning(entity ⇒ entity)
      }
    }

  override def getByLoginId(id: UUID): QIO[Login] = run(quote {
    query[Entity].filter(login ⇒ lift(id) == login.id)
  }).map {
    _.headOption match
      case None        ⇒ throw SQLException("ID NOT FOUND")
      case Some(login) ⇒ login
  }

  override def getByEmailId(id: UUID): QIO[Login] = run(quote {
    query[Entity].filter(login ⇒ lift(id) == login.emailId)
  }).map {
    _.headOption match
      case None        ⇒ throw SQLException("ID NOT FOUND")
      case Some(login) ⇒ login
  }

  override def getByContactNumberId(id: UUID): QIO[Login] =
    run(
      quote {
        query[Entity].filter(login ⇒ lift(id) == login.contactNumberId)
      }
    ).map {
      _.headOption match
        case None        ⇒ throw SQLException("ID NOT FOUND")
        case Some(login) ⇒ login
    }

  override def checkExistingId(id: UUID): QIO[Boolean] = run(quote {
    query[Entity].filter(login ⇒ lift(id) == login.id)
  }).map {
    _.headOption match
      case None        ⇒ false
      case Some(login) ⇒ true
  }

object LoginRepositoryLive:
  lazy val layer: TaskLayer[LoginRepository] =
    ZLayer(ZIO.attempt(new LoginRepositoryLive))
