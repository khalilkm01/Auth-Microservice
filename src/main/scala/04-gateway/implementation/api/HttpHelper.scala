package gateway.implementation.api

import models.common.ServerError
import models.infrastructure.Query
import zio.{ Console, RIO, Task, URIO, URLayer, ZIO, ZLayer }
import zio.http.*
import zio.http.model.{ HttpError, Method }
import zio.json.*
trait HttpHelper:
  protected val ROOT: String = "auth"

  protected def extractQuery[Req <: Query](req: Request)(using jsonDecoder: JsonDecoder[Req]): Task[Req] =
    for {
      body  <- req.body.asString
      query <- ZIO.fromEither(body.fromJson[Req].left.map(ServerError.DecodeError.apply))
    } yield query

  protected def httpResponseHandler[R, A](
    rio: RIO[R, A]
  )(using jsonEncoder: JsonEncoder[A]): URIO[R, Response] =
    rio.fold(
      {
        case ServerError.DecodeError(msg) ⇒
          Response.fromHttpError(HttpError.BadRequest(msg))
        case ServerError.NotFoundError(msg) ⇒
          Response.fromHttpError(HttpError.NotFound(msg))
        case e ⇒
          Response.fromHttpError(HttpError.InternalServerError(e.getMessage))
      },
      res ⇒ Response.json(res.toJson)
    )
