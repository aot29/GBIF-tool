package utils

import errors.{SpeciesSynonymError, SpeciesUnknownError}
import models.Types.{JSON, ValidatedSpecies}
import models.{Species, Taxon, TaxonomicStatus}
import play.api.libs.json
import play.api.libs.json.{JsObject, JsValue}

import scala.util.Try

/**
 * Parser companion to GbifService
 */
class GbifParser extends ValidatingSpeciesParser:

  /**
   * Parse the response from GBIF, as returned by GbifService
   *
   * Returns a validated result:
   *  - if the input was accepted by GBIF, then return a Species object
   *  - if the input was marked as unknown by GBIF, return a SpeciesUnknownError
   *  - if the input was marked as a synonym, i.e. species name is deprecated, return a SpeciesSynonymError
   *
   * If values are missing, replace by defaults
   * (missing values are not necessarily wrong, it just means that the animal has not yet been classified).
   * 
   * @param text response from GBIF
   * @return Species
   */
  def parse(text: JSON): ValidatedSpecies =
    val jsonVal = json.Json.parse(text)

    parseStatus(jsonVal).getOrElse(TaxonomicStatus.UNKNOWN) match
      // species was found in GBIF
      case TaxonomicStatus.ACCEPTED =>
        val gbifKey = parseUsageKey(jsonVal).getOrElse(0)
        val rank = parseRank(jsonVal).getOrElse("UNKNOWN") // Rank is Species, Genus, Familia, Ordo, etc
        // GBIF responded with GBIF key 1 or 0 ("Animal"), although it's accepted,
        // a validation error should be returned
        if gbifKey == 0 | gbifKey == 1 | rank != "SPECIES" then
          Left(SpeciesUnknownError())
        else
          Right(parseAccepted(jsonVal))
      // DOUBFUL is considered ACCEPTED
      case TaxonomicStatus.DOUBTFUL =>
        Right(parseAccepted(jsonVal))
      // Species is unknown
      case TaxonomicStatus.UNKNOWN =>
        Left(SpeciesUnknownError())
      // The following cases are all equivalent to SYNONYM
      case TaxonomicStatus.SYNONYM =>
        Left(SpeciesSynonymError())
      case TaxonomicStatus.MISAPPLIED =>
        Left(SpeciesSynonymError())
      case TaxonomicStatus.HOMOTYPIC_SYNONYM =>
        Left(SpeciesSynonymError())
      case TaxonomicStatus.PROPARTE_SYNONYM =>
        Left(SpeciesSynonymError())

  /**
   * Parsed the JSN object obtained from GBIF response,
   * into a Species object.
   * */
  private def parseAccepted(jsonVal: JsValue): Species =
    val gbifKey = parseUsageKey(jsonVal).getOrElse(0)
    Species(
      parseLatinName(jsonVal).getOrElse(""),
      parseGenus(jsonVal).getOrElse(""),
      parseFamilia(jsonVal).getOrElse(""),
      parseOrdo(jsonVal).getOrElse(""),
      gbifKey,
      jsonVal.toString
    )

  /**
   * Parse the GBIF response and return the status of the species.
   *
   * UNKNOWN: not found in GBIF, misspelled or newly discovered species
   * ACCEPTED: Species, genus, family and order are identical to the GBIF response
   * SYNONYM: Species name was deprecated in favor of a new name. The new name is in the JSON response.
   *
   * @param jsonVal a JSON object
   * @return a value taken from the Taxonomic status enum (defined in models)
   */
  private def parseStatus(jsonVal: JsValue): Try[TaxonomicStatus] =
    Try(
      jsonVal("status").as[String] match
        case "ACCEPTED" => TaxonomicStatus.ACCEPTED
        case "DOUBTFUL" => TaxonomicStatus.DOUBTFUL
        case "SYNONYM" => TaxonomicStatus.SYNONYM
        case "MISAPPLIED" => TaxonomicStatus.MISAPPLIED
        case "HOMOTYPIC_SYNONYM" => TaxonomicStatus.HOMOTYPIC_SYNONYM
        case "PROPARTE_SYNONYM" => TaxonomicStatus.PROPARTE_SYNONYM
        case "UNKNOWN" => TaxonomicStatus.UNKNOWN
    )

  private def parseLatinName(jsonVal: JsValue): Try[String] =
    Try(jsonVal("canonicalName").as[String])

  private def parseGenus(jsonVal: JsValue): Try[String] =
    Try(jsonVal("genus").as[String])
  
  private def parseFamilia(jsonVal: JsValue): Try[String] =
    Try(jsonVal("family").as[String])

  private def parseOrdo(jsonVal: JsValue): Try[String] =
    Try(jsonVal("order").as[String])

  private def parseRank(jsonVal: JsValue): Try[String] =
    Try(jsonVal("rank").as[String])

  private def parseUsageKey(jsonVal: JsValue): Try[Int] =
    Try (
      if (jsonVal.as[JsObject].keys.contains("acceptedUsageKey"))
        jsonVal("acceptedUsageKey").as[Int]
      else
        jsonVal("usageKey").as[Int]
    )
