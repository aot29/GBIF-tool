package services

import models.Species
import models.Types.{FilePath, ValidatedSpecies}

import scala.concurrent.Future
import scala.util.Try

trait SpeciesSink:
  /**
   * Save a list of species to a file.
   *
   * @return Boolean
   */
  def writeSpeciesList(speciesLIst: Seq[(Species, ValidatedSpecies)], outputPath: FilePath): Try[Unit]



