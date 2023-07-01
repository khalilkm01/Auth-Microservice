package repositories

import io.github.scottweaver.models.JdbcInfo
import zio.*
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import io.github.scottweaver.zio.testcontainers.postgresql.PostgresContainer
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.zio.aspect.DbMigrationAspect.ConfigurationCallback
import zio.test.TestAspect

import javax.sql.DataSource

object TestQuillContext:

  val containerLayer: TaskLayer[DataSource with JdbcInfo] =
    ZPostgreSQLContainer.Settings.default >+> ZPostgreSQLContainer.live

  val migrate: TestAspect[Nothing, JdbcInfo, Nothing, Any] =
    DbMigrationAspect.migrateOnce("classpath:migrations")()
