import zio.*
import zio.Console.printLine
import zio.logging.{ ConsoleLoggerConfig, LogFilter, LogFormat, consoleLogger }

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, LogFilter.logLevel(LogLevel.Info))
    )

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    Program.make
