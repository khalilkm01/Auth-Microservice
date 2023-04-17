package repositories.quill

import models.infrastructure.CommittedEvent
import repositories.EventRepository
import repositories.quill.QuillContext._

import io.getquill.*
import io.getquill.context.ZioJdbc.QIO
import io.getquill.jdbczio.Quill
import zio.*

import java.util.UUID

class EventRepositoryLive extends EventRepository:
  import QuillContext.{ *, given }

  private type Entity = CommittedEvent

  override def getById(id: UUID): QIO[Option[CommittedEvent]] = run(quote {
    query[Entity].filter(event ⇒ lift(id) == event.id)
  }).map(_.headOption)

  override def commitEvent(event: CommittedEvent): QIO[CommittedEvent] = run(
    quote {
      query[Entity]
        .insertValue(
          lift(
            event
          )
        )
        .returning(event ⇒ event)
    }
  )

object EventRepositoryLive:
  lazy val layer: TaskLayer[EventRepository] =
    ZLayer(ZIO.attempt(new EventRepositoryLive))
