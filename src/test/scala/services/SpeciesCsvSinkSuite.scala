package services

import com.typesafe.config.ConfigFactory
import errors.{SpeciesSynonymError, SpeciesUnknownError}
import models.Species
import models.Types.ValidatedSpecies

import java.io.{File, FileInputStream}
import scala.io.BufferedSource
import scala.concurrent.{Await, duration}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Using}

class SpeciesCsvSinkSuite extends munit.FunSuite:
  /** The example output file */
  val outputPath = "src/test/data/output.csv"
  /** max time to wait for each test */
  val timeout = Duration(1000, duration.MILLISECONDS)

  /** Load the CSV separator from conf/application.conf */
  private var separator: Char = ','
  override def beforeAll() =
    // load the separator char setting
    val config = ConfigFactory.load()
    separator = config.getString("csv.separator").toList.head

  // cleanup
  override def afterEach(context: AfterEach): Unit =
    // delete output file
    new File(outputPath).delete

  // example Species objects
  val speciesExamples: Seq[(Species, ValidatedSpecies)] = Seq(
    // accepted
    (Species("Puma concolor"), Right(Species("Puma concolor"))),
    (Species.Unknown, Left(SpeciesSynonymError())),
    (Species.Unknown, Left(SpeciesUnknownError()))
  )

  test("Outputfile is created") {
    // will throw FileNotFoundException if fails
    SpeciesCsvSink(separator).writeSpeciesList(speciesExamples, outputPath)
    assert(File(outputPath).exists())
  }

  test("Outputfile has correct line count") {
    // will throw FileNotFoundException if fails
    SpeciesCsvSink(separator).writeSpeciesList(speciesExamples, outputPath)
    // Open the file and read it
    Using(BufferedSource(new FileInputStream(outputPath))) { source =>
      source.getLines().toSeq
    } match
      case Success(lines) => assert(lines.length == speciesExamples.length + 1) // +1 for header line
      case Failure(ex) => fail(ex.getMessage)
  }

  test("Outputfile can be overwritten") {
    // will throw FileNotFoundException if fails
    SpeciesCsvSink(separator).writeSpeciesList(speciesExamples, outputPath)
    assert(File(outputPath).exists())
    SpeciesCsvSink(separator).writeSpeciesList(speciesExamples, outputPath)
    assert(File(outputPath).exists())
  }
