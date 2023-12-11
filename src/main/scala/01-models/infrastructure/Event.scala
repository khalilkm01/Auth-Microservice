package models.infrastructure

import models.enums.UserType
import org.joda.time.DateTime
import zio.json.JsonCodec

import java.util.UUID

sealed trait Event(channel: Channel, topic: EventTopics):
  def getChannel: Channel   = channel
  def getTopic: EventTopics = topic

final case class LoginCreatedEvent(
  loginId: UUID,
  contactNumberId: UUID,
  emailId: UUID,
  code: String,
  user: UserType
) extends Event(LoginCreatedChannel, `org.thirty7.auth.login`)
final case class LoginPasswordUpdatedEvent(
  loginId: UUID,
  user: UserType
) extends Event(LoginPasswordUpdatedChannel, `org.thirty7.auth.login`)

final case class EmailCreatedEvent(
  emailId: UUID,
  user: UserType
) extends Event(EmailCreatedChannel, `org.thirty7.auth.email`)

final case class EmailUpdatedEvent(emailId: UUID, emailAddress: String)
    extends Event(EmailUpdatedChannel, `org.thirty7.auth.email`)
final case class EmailConnectedEvent(
  emailId: UUID,
  user: UserType
) extends Event(EmailConnectedChannel, `org.thirty7.auth.email`)
final case class EmailDisconnectedEvent(
  emailId: UUID
) extends Event(channel = EmailDisconnectedChannel, topic = `org.thirty7.auth.email`)
final case class ContactNumberCreatedEvent(
  contactNumberId: UUID,
  user: UserType
) extends Event(ContactNumberCreatedChannel, `org.thirty7.auth.contact_number`)
final case class ContactNumberConnectedEvent(
  contactNumberId: UUID,
  user: UserType
) extends Event(ContactNumberConnectedChannel, `org.thirty7.auth.contact_number`)
final case class ContactNumberDisconnectedEvent(
  contactNumberId: UUID
) extends Event(channel = ContactNumberDisconnectedChannel, topic = `org.thirty7.auth.contact_number`)

object Event:

  import models.common.{ DateTimeHelper, JsonHelper }
  import JsonHelper.{ deriveCodec, given }
  import DateTimeHelper.given
  import UserType.given

  given JsonCodec[Event]                          = deriveCodec
  given JsonCodec[LoginCreatedEvent]              = deriveCodec
  given JsonCodec[LoginPasswordUpdatedEvent]      = deriveCodec
  given JsonCodec[EmailCreatedEvent]              = deriveCodec
  given JsonCodec[EmailUpdatedEvent]              = deriveCodec
  given JsonCodec[EmailConnectedEvent]            = deriveCodec
  given JsonCodec[EmailDisconnectedEvent]         = deriveCodec
  given JsonCodec[ContactNumberCreatedEvent]      = deriveCodec
  given JsonCodec[ContactNumberConnectedEvent]    = deriveCodec
  given JsonCodec[ContactNumberDisconnectedEvent] = deriveCodec
