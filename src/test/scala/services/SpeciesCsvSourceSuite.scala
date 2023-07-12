package services

import com.typesafe.config.ConfigFactory
import models.{Species, TaxonomicStatus}
import services.SpeciesCsvSource

import scala.util.{Failure, Success, Using}
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, Future}

class SpeciesCsvSourceSuite extends munit.FunSuite:
  /** The example input file */
  val inputPath = "src/test/data/input.csv"
  /** max time to wait for each test */
  val timeout = Duration(1000, MILLISECONDS)
  /** Load the CSV separator from conf/application.conf */
  private var separator: Char = ','
  override def beforeAll() =
    val config = ConfigFactory.load()
    separator = config.getString("csv.separator").toList.head

  test("example file can be opened") {
    // will throw a Exception if file not found
    val result = Await.result(
      SpeciesCsvSource(separator).listSpecies(inputPath),
      timeout)
  }

  test("Attempting to open a non-existing file fails") {
    intercept[java.io.FileNotFoundException] {
      val result = Await.result(
        SpeciesCsvSource(separator).listSpecies("non_existing_file.csv"),
        timeout)
    }
  }

  test("example file yields correct number of species") {
    val speciesList = Await.result(
      SpeciesCsvSource(separator).listSpecies(inputPath),
      timeout)
    // the example input.csv contains 100 entries
    assert(speciesList.length == 100)
  }

  test("The parsed species contain data") {
    val speciesList = Await.result(
      SpeciesCsvSource(separator).listSpecies(inputPath),
      timeout)
    for species <- speciesList do
      assert(species.latinName.nonEmpty)
      assert(species.genus.nonEmpty)
      assert(species.familia.nonEmpty)
      assert(species.ordo.nonEmpty)
      // the following entries are defaults set in the Species constructor
      assert(species.GbifUsageKey == Species.DEFAULT_KEY)
  }
