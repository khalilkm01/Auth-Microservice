package repositories

import io.getquill.context.ZioJdbc.QIO
import models.infrastructure.CommittedEvent

import java.util.UUID

trait EventRepository:
  def commitEvent(event: CommittedEvent): QIO[CommittedEvent]
  def getById(id: UUID): QIO[Option[CommittedEvent]]
