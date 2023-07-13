package clients

import models.dto.{ Auth, AuthData }
import zio.Task

trait JwtClient {
  def decodeToken(token: String): Task[Option[Auth]]
  def generateToken(auth: Auth): Task[AuthData]
}
