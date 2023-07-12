package errors

/** Errors for validation */
trait SpeciesValidationError() extends Exception
/** Thrown when the species is unknown */
case class SpeciesUnknownError() extends SpeciesValidationError
/** Thrown when the species name is deprecated, and a newer synonym should be used */
case class SpeciesSynonymError() extends SpeciesValidationError
