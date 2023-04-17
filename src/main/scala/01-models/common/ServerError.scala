package models.common

import models.infrastructure.Event
import zio.http.model.Status
import zio.http.{ Path, Response }

import java.util.UUID

sealed trait ServerError extends Throwable

object ServerError:

  final case class DecodeError(str: String) extends ServerError

  final case class InternalServerError(error: InternalServerErrorMessage) extends ServerError

  final case class NotFoundError(str: String) extends ServerError

  final case class ServiceError(error: ServiceErrorMessage) extends ServerError

  final case class ClientRequestError(status: Status, req: Response, endpoint: String) extends ServerError

  final case class PaymentError(error: PaymentErrorMessage) extends ServerError

  final case class EventNotMatchedError(event: Event) extends ServerError

  case class PaymentProviderEnumConversionError(paymentProvider: String) extends ServerError

  enum EnumConversionError(value: String) extends ServerError:
    case CountryConversionError(country: String)         extends EnumConversionError(country)
    case CountryCodeConversionError(countryCode: String) extends EnumConversionError(countryCode)
    case UserTypeConversionError(user: String)           extends EnumConversionError(user)

  enum SQLExceptionMessage(message: String) extends ServerError:
    case IDNotFound(id: UUID) extends SQLExceptionMessage(s"Model with ID: $id, does not exist")
    case EntityNotFound(parameter: String, value: String)
        extends SQLExceptionMessage(s"Entity with $parameter: $value, does not exist")
    case EntityDoesNotExist(entity: String) extends SQLExceptionMessage(s"Entity: $entity, does not exist")
    case EnumConversionError(value: String) extends SQLExceptionMessage(s"Could not convert value: $value, to an enum")
    case MultipleEntityEnumConversionError
        extends SQLExceptionMessage("Enum conversion error(s) occurred while converting entities")

  enum InternalServerErrorMessage(msg: String) extends ServerError:
    case IllegalArgumentMessage extends InternalServerErrorMessage("Illegal Argument Exception Occurred")
    case SQLError(msg: String)
        extends InternalServerErrorMessage(
          s"Internal Server Error Occurred with Exception: $msg"
        )
    case UnknownError(msg: String) extends InternalServerErrorMessage(msg)
    case ClientError(url: String)  extends InternalServerErrorMessage(s"Client could not be requested: $url")
    case ClientErrorWithMsg(url: String, msg: String)
        extends InternalServerErrorMessage(s"Client could not be requested: $url. Failed with message: $msg")

  enum PaymentErrorMessage(msg: String):
    case PaymentFailedError(msg: String) extends PaymentErrorMessage(s"Payment Failed with Error Message: $msg")

  enum ServiceErrorMessage(msg: String):
    case InternalServiceError(err: String) extends ServiceErrorMessage(s"Service failed because: $err")
    case IdNotFound(id: UUID)              extends ServiceErrorMessage(s"Model with ID: $id, does not exist")
    case UnauthorizedAccessError(id: UUID)
        extends ServiceErrorMessage(
          s"Unauthorised Access attempted on model with ID: $id"
        )
    case LoginError                     extends ServiceErrorMessage("Login Failed")
    case ExistingModelIdFound(id: UUID) extends ServiceErrorMessage(s"Model with ID: $id")
    case IllegalServiceCall             extends ServiceErrorMessage("Illegal Service Call Occurred")
