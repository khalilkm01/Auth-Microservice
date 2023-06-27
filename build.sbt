ThisBuild / scalaVersion     := "3.2.0"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "org.thirty7.auth"
ThisBuild / organizationName := "thirty7"
ThisBuild / name             := "auth"

lazy val root = (project in file("."))
  .settings(
    name        := "auth",
    Test / fork := true
  )

libraryDependencies ++= {
  val nscalaTimeVersion  = "2.32.0"
  val scalaBcryptVersion = "4.3.0"
  val postgresqlVersion  = "42.5.4"
  val quillVersion       = "4.6.0.1"
  val flywayVersion      = "9.16.0"
  val zioVersion         = "2.0.10"
  val zioKafkaVersion    = "2.1.3"
  val zioSchemaVersion   = "0.4.8"
  val zioJsonVersion     = "0.4.2"
  val zioJsonJwtVersion  = "9.2.0"
  val zioConfigVersion   = "3.0.7"
  val zioHttpVersion     = "0.0.5"
  val zioLoggingVersion  = "2.1.11"

  val testContainersVersion = "0.10.0"

  Seq(
    "com.github.nscala-time" %% "nscala-time"              % nscalaTimeVersion,
    "com.github.t3hnar"       % "scala-bcrypt_2.13"        % scalaBcryptVersion,
    "com.github.jwt-scala"   %% "jwt-zio-json"             % zioJsonJwtVersion,
    "io.getquill"            %% "quill-jdbc-zio"           % quillVersion,
    "org.flywaydb"            % "flyway-core"              % flywayVersion,
    "org.postgresql"          % "postgresql"               % postgresqlVersion,
    "dev.zio"                %% "zio"                      % zioVersion,
    "dev.zio"                %% "zio-kafka"                % zioKafkaVersion,
    "dev.zio"                %% "zio-schema"               % zioSchemaVersion,
    "dev.zio"                %% "zio-http"                 % zioHttpVersion,
    "dev.zio"                %% "zio-json"                 % zioJsonVersion,
    "dev.zio"                %% "zio-config"               % zioConfigVersion,
    "dev.zio"                %% "zio-config-typesafe"      % zioConfigVersion,
    "dev.zio"                %% "zio-config-magnolia"      % zioConfigVersion,
    "dev.zio"                %% "zio-logging-slf4j-bridge" % zioLoggingVersion,
    "dev.zio"                %% "zio-test"                 % zioVersion % Test,
    "dev.zio"                %% "zio-test-sbt"             % zioVersion % Test,
    "dev.zio"                %% "zio-test-magnolia"        % zioVersion % Test,
//    "dev.zio"                %% "zio-test-junit"                    % zioVersion            % Test,
    "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % testContainersVersion % Test
    //  "dev.zio" %% "zio-logging" % "2.1.0",
    //  "dev.zio" %% "zio-logging-slf4j" % "2.1.0",
    //  "org.slf4j" % "slf4j-log4j12" % "1.7.36",

  )
}

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
