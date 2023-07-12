package utils

import models.Species
import models.Types.ValidatedSpecies

/** 
 * Base trait for any service that parses the response 
 * obtained from an external reference taxonomy 
 * into a Species object 
 */
trait SpeciesParser:
  def parse(text: String): Species

trait ValidatingSpeciesParser:
  def parse(text: String): ValidatedSpecies
