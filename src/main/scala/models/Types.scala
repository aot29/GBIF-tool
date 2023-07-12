package models

import errors.SpeciesValidationError

object Types {
  /** an alias for JSON strings */
  type JSON = String
  type FilePath = String
  type ValidatedSpecies = Either[SpeciesValidationError, Species]
}
