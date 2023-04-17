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

  given MappedEncoding[UUID, String] =
    MappedEncoding[UUID, String](_.toString)
  given MappedEncoding[String, UUID] =
    MappedEncoding[String, UUID](UUID.fromString)

  given MappedEncoding[DateTime, Date] =
    MappedEncoding[DateTime, Date](_.toDate)
  given MappedEncoding[Date, DateTime] =
    MappedEncoding[Date, DateTime](DateTime(_, DateTimeZone.UTC))

  given MappedEncoding[CountryCode, String] =
    MappedEncoding[CountryCode, String](_.toString)
  given MappedEncoding[String, CountryCode] =
    MappedEncoding[String, CountryCode](CountryCode.fromString)

  given MappedEncoding[UserType, String] =
    MappedEncoding[UserType, String](_.toString)
  given MappedEncoding[String, UserType] =
    MappedEncoding[String, UserType](UserType.fromString)

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
