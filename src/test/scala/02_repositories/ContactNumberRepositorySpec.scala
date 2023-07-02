package repositories

import models.enums.{ CountryCode, UserType }
import models.persisted.ContactNumber

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.models.JdbcInfo
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource

object ContactNumberRepositorySpec extends ZIOSpecDefault {
  private lazy val layer: TaskLayer[ContactNumberRepository with DataSource with JdbcInfo] =
    TestQuillContext.containerLayer >+> quill.ContactNumberRepositoryLive.layer

  private val baseContactNumberModel: ContactNumber = ContactNumber(
    id = UUID.randomUUID(),
    countryCode = CountryCode.US,
    digits = "5555555555",
    userType = UserType.ADMIN,
    connected = false,
    createdAt = DateTime.now(),
    updatedAt = DateTime.now()
  )

  def spec: Spec[Any, Throwable] = {
    suite("ContactNumberRepositorySpec")(
      test("Contact number is retrieved by number, countryCode and user type") {
        for {
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          contactNumberModel = baseContactNumberModel.copy(digits = "6666666666") // another random valid number
          newContactNumber <- contactNumberRepository.create(contactNumberModel)
          contactNumber <- contactNumberRepository.getByNumber(
            newContactNumber.countryCode,
            newContactNumber.digits,
            newContactNumber.userType
          )
        } yield assertTrue(contactNumber == newContactNumber)
      },
      test("Contact number existing id check works") {
        for {
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          contactNumberModel = baseContactNumberModel.copy()
          newContactNumber <- contactNumberRepository.create(contactNumberModel)
          exists           <- contactNumberRepository.checkExistingId(newContactNumber.id)
        } yield assertTrue(exists)
      },
      test("Contact number non-existing id check returns false") {
        for {
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          nonExistentId = UUID.randomUUID()
          exists <- contactNumberRepository.checkExistingId(nonExistentId)
        } yield assertTrue(!exists)
      },
      test("Contact number existing number check works") {
        for {
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          contactNumberModel = baseContactNumberModel.copy(digits = "7777777777") // another random valid number
          newContactNumber <- contactNumberRepository.create(contactNumberModel)
          exists <- contactNumberRepository.checkExistingNumber(
            newContactNumber.countryCode,
            newContactNumber.digits,
            newContactNumber.userType
          )
        } yield assertTrue(exists)
      },
      test("Contact number non-existing number check returns false") {
        for {
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          exists <- contactNumberRepository.checkExistingNumber(
            CountryCode.NG,
            "8888888888",
            UserType.CUSTOMER
          ) // non-existent number
        } yield assertTrue(!exists)
      },
      test("Contact number connection works") {
        for
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          contactNumberModel = baseContactNumberModel.copy(digits = "9999999999") // another random valid number
          newContactNumber <- contactNumberRepository.create(contactNumberModel)
          connected        <- contactNumberRepository.connectNumber(newContactNumber.id)
        yield assertTrue(connected)
      },
      test("Contact number disconnection works") {
        for
          contactNumberRepository <- ZIO.service[ContactNumberRepository]
          contactNumberModel = baseContactNumberModel.copy(digits = "1010101010") // another random valid number
          newContactNumber <- contactNumberRepository.create(contactNumberModel)
          _                <- contactNumberRepository.connectNumber(newContactNumber.id)
          disconnected     <- contactNumberRepository.disconnectNumber(newContactNumber.id)
        yield assertTrue(disconnected)

      }
    ) @@ TestAspect.parallelN(6) @@ DbMigrationAspect.migrateOnce("classpath:migrations")()
  }.provideShared(layer)
}
