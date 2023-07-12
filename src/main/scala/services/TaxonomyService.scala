package services

import models.Species
import models.Types.JSON

import scala.concurrent.Future
/**
 * Base trait for any service that calls an external reference taxonomy 
 */
trait TaxonomyService:
  /**
   * @param name latin name of a species
   * @return JSON string
   */
  def matchSpecies(species: Species): Future[JSON]

  /**
   * @param name list of latin names of species
   * @return list of JSON strings
   */
  def matchSpeciesList(species: Seq[Species]): Future[Seq[JSON]]