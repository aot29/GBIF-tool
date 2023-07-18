package utils

import errors.{SpeciesUnknownError, SpeciesValidationError}
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
          // matchName -> Validated Species OK -> return a string representing the species
          case Right(obj) => Future.successful(Report(obj.toString))
          // matchName -> Validated Species FAIL -> return an exception with the message in Left
          case Left(ex: SpeciesValidationError) => Future.failed(ex)
          // matchAll -> Seq of ValidatedSpecies -> returns a Summary as string encapsulated in a Report
          case obj: Summary => Future.successful(Report(obj.toString))
      // processing failed
      case Failure(ex) => Future.failed(ex)
      // for completeness
      case null => Future.failed(new Exception("Unknown error"))
    }