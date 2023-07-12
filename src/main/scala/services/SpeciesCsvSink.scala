package services

import errors.{SpeciesSynonymError, SpeciesUnknownError}
import models.{Species, TaxonomicStatus}
import models.Types.{FilePath, ValidatedSpecies}

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}
import scala.concurrent.Future
import scala.util.{Failure, Try, Using}

/**
 * Writes a list of species to a CSV file containing a table of one species per line.
 * Columns in the CSV file should include (at least, in that order): "species", "genus", "familia", "ordo", "GbifUsageKey"
 * The separator char is set in conf/aplication.conf
 * */
class SpeciesCsvSink(separator: Char) extends SpeciesSink:

  /**
   * Save a list of species to a file.
   *
   * @return Boolean
   */
  def writeSpeciesList(speciesList: Seq[(Species, ValidatedSpecies)], outputPath: FilePath): Try[Unit] =
    if speciesList.isEmpty then
      Failure(IllegalArgumentException("Species list is empty"))

    else
      // put header line together
      val headers = Seq("species", "genus", "familia", "ordo", "GbifUsageKey", "Taxonomic status")
      val headerLine = headers.mkString(separator.toString)

      // put content lines together
      var lines: scala.collection.mutable.Seq[String] = scala.collection.mutable.Seq.empty[String]
      for species <- speciesList do
        val originalSpecies = species._1
        val validatedSpecies = species._2
        val line = validatedSpecies match
          // validation failed: create a line with original species name and reason for failure
          case Left(ex: SpeciesSynonymError) =>
            Seq(originalSpecies.latinName, "", "", "", "", "", TaxonomicStatus.SYNONYM.toString)
          case Left(ex: SpeciesUnknownError) =>
            Seq(originalSpecies.latinName, "", "", "", "", "", TaxonomicStatus.UNKNOWN.toString)
          case Left(_) =>
            Seq(originalSpecies.latinName, "", "", "", "", "", "System error")
          case Right(species: Species) =>
            Seq(species.latinName, species.genus, species.familia, species.ordo, species.GbifUsageKey, TaxonomicStatus.ACCEPTED.toString)
        lines = lines :+ line.mkString(separator.toString)

      // write to file
      Using(new BufferedWriter(new FileWriter(File(outputPath)))) { writer =>
        writer.write(headerLine)
        writer.newLine()
        for line <- lines do
          writer.write(line)
          writer.newLine()
      }



