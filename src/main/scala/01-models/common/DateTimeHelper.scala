package models.common

import org.joda.time.DateTime
import zio.json.{ JsonDecoder, JsonEncoder }

object DateTimeHelper:
  given JsonDecoder[DateTime] = JsonDecoder[String].map(DateTime(_))

  given JsonEncoder[DateTime] = JsonEncoder[String].contramap(_.toString)
