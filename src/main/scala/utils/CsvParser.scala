package utils

import models.Species
import models.Types.ValidatedSpecies

/**
 * The parser used to parse a single line of the table into a Species object
 * The separator char is set in conf/aplication.conf
 *
 * @param separator
 */
class CsvParser(separator: Char) extends SpeciesParser:
  override def parse(line: String): Species =
    val splits = line.split(separator).map(_.trim).map(_.replace("\"", ""))
    Species(splits(0), splits(1), splits(2), splits(3))

  def getHeaders(headerLine: String): Seq[String] =
    headerLine.split(separator).map(_.trim).map(_.replace("\"", ""))