package models

/**
 * Status taken from GBIF
 * @see: https://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/TaxonomicStatus.html
 * */
enum TaxonomicStatus:
  case ACCEPTED, SYNONYM, UNKNOWN, DOUBTFUL, MISAPPLIED, HOMOTYPIC_SYNONYM, PROPARTE_SYNONYM

/**
 * A class representing an animal species.
 * */
class Species(
   val latinName: String,
   val genus: String,
   val familia: String,
   val ordo: String,
   val GbifUsageKey: Int
  ):
  
  /**
   * Overloaded constructor with defaults, used when reading the input CSV file, that might be incomplete
   */
  def this(latinName: String, genus: String, familia: String, ordo: String) =
    this(latinName, genus, familia, ordo, Species.DEFAULT_KEY)

  def this(latinName: String) =
    this(latinName, "", "", "", Species.DEFAULT_KEY)

  /**
   * Comparator. Two species are equal if their latinName or their GbifUsageKey are the same.
   * The taxonomy may not be complete, as either "ordo" is missing for some species (e.g. turtles)
   * or the species has not been classified yet.
   *
   * @param that any object
   * @return true if equal, false if different
   */
  override def equals(that: Any): Boolean =
    that match
      case that:Species =>
        (that.latinName == this.latinName & that.latinName.nonEmpty)
        |
        (that.GbifUsageKey == this.GbifUsageKey & that.GbifUsageKey!=Species.DEFAULT_KEY)
      case _ => false

  /**
   * Pretty-print to the console. Skip printing the JSON.
   */
  override def toString =
    s"""
      latin name:   $latinName
      genus:        $genus
      family:       $familia
      ordo:         $ordo
      GbifUsageKey: $GbifUsageKey
      |""".stripMargin

object Species:
  /** The GBIF key for the animal kingdom, used as default */
  val DEFAULT_KEY = 1

  /** Unknown animal */
  val Unknown: Species = Species("Animalia", "", "", "")
