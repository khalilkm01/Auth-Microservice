import config.Config.DBConfig

import zio.{ Task, ZIO }
import org.flywaydb.core.Flyway

object FlywayMigration:
  def migrate(config: DBConfig): Task[Unit] =
    ZIO.logInfo("Starting Flyway Migrations") *>
      ZIO
        .attemptBlocking(
          Flyway
            .configure(this.getClass.getClassLoader)
            .dataSource(config.url, config.user, config.password)
            .locations("classpath:migrations")
            .connectRetries(30000)
            .load()
            .migrate()
        ) *> ZIO.logInfo("Migrations Complete")
