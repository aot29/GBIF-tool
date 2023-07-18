package models

import models.Types.JSON

/**
 * Status taken from GBIF
 *
 * @see: https://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/TaxonomicStatus.html
 * */
enum TaxonomicStatus:
  case ACCEPTED, SYNONYM, UNKNOWN, DOUBTFUL, MISAPPLIED, HOMOTYPIC_SYNONYM, PROPARTE_SYNONYM

sealed trait Property

/** A binomial name contains only letters in the latin alphabet or spaces or points or male/female symbols, and at least 2 words
 * e.g. "Anser anser" (goose), "Animalia" (some animal), "E. africanus asinus ♂ × E. ferus caballus ♀" (a mule)
 * are considered valid names.
 * */
case class Binomial(value: String) extends Property:
  require(value == "Animalia" | value.matches("[a-zA-Z\\s\\.♂♀]+"))
  override def toString: String = value

/** A taxon name contains only letters in the latin alphabet, no spaces (or is empty) */
case class Taxon(value: String) extends Property:
  def nonEmpty: Boolean =
    value.nonEmpty
  override def toString: String = value
object Taxon:
  val empty: Taxon = Taxon("")

/** A key must be a positive Integer*/
case class GbifUsageKey(value:Int) extends Property:
  require(value>=0)
  // compare to int
  def ==(that: Int): Boolean =
    value.toInt == that
  def !=(that: Int): Boolean =
    value.toInt != that
  override def toString: String = value.toString

/**
 * A class representing an animal species.
 * */
case class Species(
   val latinName: Binomial,
   val genus: Taxon,
   val familia: Taxon,
   val ordo: Taxon,
   val GbifUsageKey: GbifUsageKey,
   val json: JSON
  ):

  /**
   * Comparator. Two species are equal if their latinName or their GbifUsageKey are the same.
   * The taxonomy may not be complete, as either "ordo" is missing for some species (e.g. turtles)
   * or the species has not been classified yet.
   *
   * @param that any object
   * @return true if equal, false if different
   */
  // after reading https://alvinalexander.com/scala/how-to-define-equals-hashcode-methods-in-scala-object-equality/
  // decided to not implement this, as Species is now a case class anyway
/*
  override def equals(that: Any): Boolean =
    that match
      case that:Species =>
        (that.latinName == this.latinName)
        |
        (that.GbifUsageKey == this.GbifUsageKey & that.GbifUsageKey!=Species.DEFAULT_KEY)
      case _ => false
*/
  /**
   * Pretty-print to the console. Skip printing the JSON.
   */
  override def toString: String =
    s"""
      latin name:   $latinName
      genus:        $genus
      family:       $familia
      ordo:         $ordo
      GbifUsageKey: $GbifUsageKey
      Gbif response: $json
       |""".stripMargin

object Species:
  /** The GBIF key for the animal kingdom, used as default */
  val DEFAULT_KEY: GbifUsageKey = GbifUsageKey(1)

  /** Unknown animal */
  val Unknown: Species = Species(Binomial("Animalia sp."))

  /**
   * Overloaded constructor with defaults, used when reading the input CSV file, that might be incomplete
   */
  def apply(latinNameString: String, genusString: String, familiaString: String, ordoString: String, GbifUsageKeyInt: Int, json: JSON): Species =
    Species(Binomial(latinNameString), Taxon(genusString), Taxon(familiaString), Taxon(ordoString), GbifUsageKey(GbifUsageKeyInt), json)

  def apply(latinName: Binomial, genus: Taxon, familia: Taxon, ordo: Taxon): Species =
    Species(latinName, genus, familia, ordo, Species.DEFAULT_KEY, "")

  def apply(latinNameString: String, genusString: String, familiaString: String, ordoString: String): Species =
    Species(Binomial(latinNameString), Taxon(genusString), Taxon(familiaString), Taxon(ordoString), Species.DEFAULT_KEY, "")

  def apply(latinName: Binomial): Species =
    Species(latinName, Taxon.empty, Taxon.empty, Taxon.empty, Species.DEFAULT_KEY, "")

  def apply(latinNameString: String): Species =
    Species(Binomial(latinNameString), Taxon.empty, Taxon.empty, Taxon.empty, Species.DEFAULT_KEY, "")
