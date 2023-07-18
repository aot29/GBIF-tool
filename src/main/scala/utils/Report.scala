package utils

import models.Summary
import models.Types.ValidatedSpecies

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.control.NonFatal
import concurrent.ExecutionContext.Implicits.global

class Report(val value: String):
  override def toString: String = value


/**
 * Compiles a printable "report" from either
 * a ValidatedSpecies or a sequence of ValidatedSpecies.
 * */
object Report:
  def from[A](futureObj: Future[A]): Future[Report] =
    futureObj.transformWith {
      case Success(obj) =>
        obj match
          case Right(obj) => Future.successful(Report(obj.toString))
          case Left(ex: String) => Future.failed(new Exception(ex))
          case obj: Any => Future.successful(Report(obj.toString))
      case Failure(ex) => Future.failed(ex)
      case null => Future.failed(new Exception("Unknown error"))
    }