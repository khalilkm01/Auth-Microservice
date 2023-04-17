package models.common

import zio.http.Response

final case class ZIOResponse[Res](originalResAsString: String, res: Res)
