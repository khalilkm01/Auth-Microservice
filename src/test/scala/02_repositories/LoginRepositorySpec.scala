package repositories

import models.enums.UserType
import models.persisted.Login

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.models.JdbcInfo
import org.joda.time.DateTime

import java.util.UUID
import javax.sql.DataSource

object LoginRepositorySpec extends ZIOSpecDefault:

  private val baseLoginModel: Login = Login(
    id = UUID.randomUUID,
    password = "randomString",
    blocked = false,
    userType = UserType.ADMIN,
    emailId = UUID.randomUUID,
    contactNumberId = UUID.randomUUID,
    createdAt = DateTime.now(),
    updatedAt = DateTime.now()
  )

  def spec: Spec[Any, Throwable] =
    suite("LoginRepositorySuite")(
      specSkeleton("Live").provideSomeLayer(quill.LoginRepositoryLive.layer),
      specSkeleton("InMemory").provideSomeLayer(inmemory.LoginRepositoryInMemory.layer)
    ).provideShared(TestQuillContext.containerLayer)

  def specSkeleton(label: String): Spec[LoginRepository with DataSource with JdbcInfo, Throwable] =
    suite(s"LoginRepository${label}Spec")(
      test("Login is created and retrieved") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          login <- loginRepository.getById(newLogin.id)
        } yield assertTrue(login == newLogin)
      },
      test("Login is updated by changing password") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          updatedLogin <- loginRepository.save(
            newLogin.copy(password = "newPassword")
          )
          login <- loginRepository.getById(newLogin.id)
        } yield assertTrue(updatedLogin.password == "newPassword" && login == updatedLogin)
      },
      test("Login Deletes") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          _     <- loginRepository.delete(List(newLogin.id))
          login <- loginRepository.checkExistingId(newLogin.id)
        } yield assertTrue(!login)
      },
      test("Login is retrieved by emailId") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          login <- loginRepository.getByEmailId(newLogin.emailId)
        } yield assertTrue(login == newLogin)
      },
      test("Login is retrieved by contactNumberId") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          login <- loginRepository.getByContactNumberId(newLogin.contactNumberId)
        } yield assertTrue(login == newLogin)
      },
      test("Login check existing id work") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          loginModel = baseLoginModel.copy(
            emailId = UUID.randomUUID,
            contactNumberId = UUID.randomUUID
          )
          newLogin <- loginRepository.create(
            loginModel
          )
          login <- loginRepository.checkExistingId(newLogin.id)
        } yield assertTrue(login)
      },
      test("Checking a non-existing id should return false") {
        for {
          loginRepository <- ZIO.service[LoginRepository]
          nonExistentId = UUID.randomUUID
          exists <- loginRepository.checkExistingId(nonExistentId)
        } yield assertTrue(!exists)
      }
    ) @@ TestAspect.parallel @@ DbMigrationAspect.migrateOnce("classpath:migrations")()
