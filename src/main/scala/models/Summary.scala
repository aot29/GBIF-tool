package models
import errors.{SpeciesSynonymError, SpeciesUnknownError}
import models.Types.ValidatedSpecies

/**
 * Represents a summary of a list of species validated against an external reference taxonomy.
 * */
class Summary (val acceptedCount: Int, val synonymCount: Int, val unknownCount: Int, val failureCount: Int):
  /**
   * Pretty-print a summary
   * */
  override def toString: String =
    s"""
      Accepted:     $acceptedCount
      Synonym:      $synonymCount
      Unknown:      $unknownCount
      Failures:     $failureCount
    |""".stripMargin

object Summary:
  /**
   * Construct Summary object from validated species list
   *
   * @param speciesList A list of validated species
   */
  def fromSpeciesList(speciesList: Seq[(Species, ValidatedSpecies)]) =
    var _acceptedCount = 0
    var _synonymCount = 0
    var _unknownCount = 0
    var _failureCount = 0
    for species <- speciesList do
      val originalSpecies = species._1
      val validatedSpecies = species._2
      validatedSpecies match
        case Right(validatedSpecies) => _acceptedCount += 1
        case Left(SpeciesSynonymError()) => _synonymCount += 1
        case Left(SpeciesUnknownError()) => _unknownCount += 1
        case Left(_) => _failureCount += 1
    Summary(_acceptedCount, _synonymCount, _unknownCount, _failureCount)
