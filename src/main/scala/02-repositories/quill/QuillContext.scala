package repositories.quill

import models.enums.{ CountryCode, UserType }
import io.getquill.*
import io.getquill.jdbczio.Quill
import org.joda.time.{ DateTime, DateTimeZone }
import zio.*

import java.util.{ Date, UUID }
import javax.sql.DataSource

final case class QuillContext() extends PostgresZioJdbcContext(SnakeCase)

object QuillContext extends PostgresZioJdbcContext(SnakeCase):
  val dataSourceLayer: ULayer[DataSource] =
    Quill.DataSource.fromPrefix("postgres").orDie

  given MappedEncoding[DateTime, Date] =
    MappedEncoding[DateTime, Date](_.toDate)
  given MappedEncoding[Date, DateTime] =
    MappedEncoding[Date, DateTime](DateTime(_, DateTimeZone.UTC))

  given Encoder[CountryCode] =
    encoder(java.sql.Types.OTHER, (index, value, row) => row.setObject(index, value, java.sql.Types.OTHER))
  given Decoder[CountryCode] =
    decoder(row ⇒ index ⇒ CountryCode.fromNum(row.getObject(index).toString))

  given Encoder[UserType] =
    encoder(java.sql.Types.OTHER, (index, value, row) => row.setObject(index, value, java.sql.Types.OTHER))
  given Decoder[UserType] =
    decoder(row ⇒ index ⇒ UserType.fromString(row.getObject(index).toString))

  implicit final class InstantOps(dt: DateTime):
    def >(other: DateTime): Quoted[Boolean] = quote(
      sql"$dt > $other".asCondition
    )
    def <(other: DateTime): Quoted[Boolean] = quote(
      sql"$dt < $other".asCondition
    )
    def ==(other: DateTime): Quoted[Boolean] = quote(
      sql"$dt == $other".asCondition
    )
    def >=(other: DateTime): Quoted[Boolean] = quote(
      sql"$dt >= $other".asCondition
    )
    def <=(other: DateTime): Quoted[Boolean] = quote(
      sql"$dt <= $other".asCondition
    )
    def between(min: DateTime, max: DateTime): Quoted[Boolean] = quote(
      sql"$dt BETWEEN $min AND $max".asCondition
    )

//object QuillContextLayer:
//  val layer: ZLayer[Any, Throwable, QuillContext] =
//    ZLayer.scoped(QuillContextLive())
