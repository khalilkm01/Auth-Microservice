package clients

import models.common.{ Auth, AuthData }
import zio.Task

trait JwtClient {
  def decodeToken(token: String): Task[Option[Auth]]
  def generateToken(auth: Auth): Task[AuthData]
}
