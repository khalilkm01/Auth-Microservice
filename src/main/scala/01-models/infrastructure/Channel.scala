package models.infrastructure

import zio.json.{ DeriveJsonCodec, JsonCodec }

sealed trait Channel

case object LoginCreatedChannel              extends Channel
case object LoginPasswordUpdatedChannel      extends Channel
case object EmailCreatedChannel              extends Channel
case object EmailUpdatedChannel              extends Channel
case object EmailConnectedChannel            extends Channel
case object EmailDisconnectedChannel         extends Channel
case object ContactNumberCreatedChannel      extends Channel
case object ContactNumberUpdatedChannel      extends Channel
case object ContactNumberConnectedChannel    extends Channel
case object ContactNumberDisconnectedChannel extends Channel

object Channel:
  given JsonCodec[Channel] = DeriveJsonCodec.gen
