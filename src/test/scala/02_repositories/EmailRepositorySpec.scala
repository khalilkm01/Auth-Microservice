package repositories

import models.enums.UserType
import models.persisted.Email

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.models.JdbcInfo
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource

object EmailRepositorySpec extends ZIOSpecDefault {
  private lazy val layer: TaskLayer[EmailRepository with DataSource with JdbcInfo] =
    TestQuillContext.containerLayer >+> quill.EmailRepositoryLive.layer

  private val baseEmailModel: Email = Email(
    id = UUID.randomUUID(),
    emailAddress = "test@example.com",
    verified = false,
    connected = false,
    userType = UserType.ADMIN,
    createdAt = DateTime.now(),
    updatedAt = DateTime.now()
  )

  def spec: Spec[Any, Throwable] = {
    suite("EmailRepositorySpec")(
      test("Should retrieve Email by email address and user type") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          emailModel = baseEmailModel.copy(emailAddress = "newTest@example.com")
          newEmail <- emailRepository.create(emailModel)
          email    <- emailRepository.getByEmailAddress(newEmail.emailAddress, newEmail.userType)
        } yield assertTrue(email == newEmail)
      },
      test("Should return true when checking an existing id") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          emailModel = baseEmailModel.copy()
          newEmail <- emailRepository.create(emailModel)
          exists   <- emailRepository.checkExistingId(newEmail.id)
        } yield assertTrue(exists)
      },
      test("Should return false when checking a non-existing id") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          nonExistentId = UUID.randomUUID()
          exists <- emailRepository.checkExistingId(nonExistentId)
        } yield assertTrue(!exists)
      },
      test("Should return true when checking an existing email address and user type") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          emailModel = baseEmailModel.copy(emailAddress = "newTest2@example.com")
          newEmail <- emailRepository.create(emailModel)
          exists   <- emailRepository.checkExistingEmailAddress(newEmail.emailAddress, newEmail.userType)
        } yield assertTrue(exists)
      },
      test("Should return false when checking a non-existing email address and user type") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          exists          <- emailRepository.checkExistingEmailAddress("nonExisting@example.com", UserType.ADMIN)
        } yield assertTrue(!exists)
      },
      test("Should handle connectEmail correctly") {
        for {
          emailRepository <- ZIO.service[EmailRepository]
          emailModel = baseEmailModel
          newEmail <- emailRepository.create(emailModel)
          _        <- emailRepository.connectEmail(newEmail.id)
          email    <- emailRepository.getById(newEmail.id)
        } yield assertTrue(email.connected)

      }
    ) @@ TestAspect.parallelN(6) @@ DbMigrationAspect.migrateOnce("classpath:migrations")()
  }.provideShared(layer)
}
