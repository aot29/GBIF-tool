package services

import models.Types.{FilePath, ValidatedSpecies}
import models.{Species, TaxonomicStatus}
import utils.{CsvParser, SpeciesParser}

import java.io.{Closeable, FileInputStream, InputStream}
import scala.concurrent.Future
import scala.io.{BufferedSource, Codec}
import java.util.NoSuchElementException
import scala.collection.immutable.List
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try, Using}

/**
 * Reads a CSV file containing a table of species, one species per line.
 * Columns in the CSV file should include (at least, in that order): "species", "genus", "familia", "ordo"
 * The separator char is set in conf/aplication.conf
 * */
class SpeciesCsvSource(separator: Char) extends SpeciesSource:
  /** The parser used to parse a single line of the table into a Species object */
  val parser = CsvParser(separator)
  /** The headers expected in the CSV file*/
  val expectedHeaders = Seq("species", "genus", "familia", "ordo")

  /**
   * Override abstract method in trait SpeciesSource
   * Wrap a BufferedSource
   *
   *  @return List of Species objects
   */
  override def listSpecies(inputPath: FilePath): Future[Seq[Species]] =
    // Open the file and read it
    val lines = Using(BufferedSource(new FileInputStream(inputPath))) { source =>
      source.getLines().toSeq
    }

    lines match
      case Failure(ex) => Future.failed(ex)
      // file loaded, proceed to parsing
      case Success(lines) =>
        // check that input file is not empty
        if lines.isEmpty then Future.failed(IllegalArgumentException("Empty input file"))
        else
          // check that first line contains correct headers
          val headers = parser.getHeaders(lines.head)
          if expectedHeaders.diff(headers).nonEmpty then
            Future.failed(IllegalArgumentException("File does not have correct header line"))
          else
            // a mutable, empty, list of Species
            var mutableSpeciesList: scala.collection.mutable.Seq[Species] = scala.collection.mutable.Seq.empty[Species]
            for line <- lines.tail do
              mutableSpeciesList = mutableSpeciesList :+ parser.parse(line)
            // return the species list, but first cast it to an immutable list
            Future.successful(mutableSpeciesList.toSeq)
