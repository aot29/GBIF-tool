package models

import errors.{SpeciesSynonymError, SpeciesUnknownError}
import models.Types.ValidatedSpecies

class SummarySuite extends munit.FunSuite:
  val speciesExamples: Seq[(Species, ValidatedSpecies)] = Seq(
    // accepted
    (Species("Puma concolor"), Right(Species("Puma concolor"))),
    (Species.Unknown, Left(SpeciesSynonymError())),
    (Species.Unknown, Left(SpeciesUnknownError()))
  )

  test("Summary total count should be count in list") {
    val summary = Summary.fromSpeciesList(speciesExamples)
    assert(summary.acceptedCount + summary.synonymCount + summary.unknownCount + summary.failureCount == speciesExamples.length)
  }
