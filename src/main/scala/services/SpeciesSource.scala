package services

import models.Species
import models.Types.{FilePath, ValidatedSpecies}

import scala.concurrent.Future
import scala.util.Try

trait SpeciesSource:
  /**
   * Lists all the species in the source file.
   *
   * @return List of Species objects
   */
  def listSpecies(inputPath: FilePath): Future[Seq[Species]]



