package services

import com.typesafe.config.ConfigFactory
import controllers.GbifToolController.{config, maxAttempts}
import models.Types.JSON
import models.{Species, TaxonomicStatus}

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, MILLISECONDS}

class GbifServiceSuite extends munit.FunSuite:
  val speciesExamples = Seq(
    // accepted
    Species("Puma concolor"),
    Species("Bufo americanus"),
    Species("Animalia"),
    Species("Aldabrachelys gigantea"),
    Species("Anoplotrupes stercorosus")
  )

/** max time to wait for each test */
  val timeout = Duration(10000, MILLISECONDS)
  /**
   * Load the endpoint URL and maxAttempts from conf/application.conf
   * before running any tests.
   */
  private var endpoint: String = null
  private var maxAttempts: Int = 3
  override def beforeAll() =
    val config = ConfigFactory.load()
    endpoint = config.getString("GBIF_URL_SPECIES_ENDPOINT")
    maxAttempts = config.getInt("maxAttempts")

  test("Check that API is reachable") {
    // will throw IOException if fail to contact server
    val response = Await.result(
      GbifService(endpoint, maxAttempts).matchSpecies(Species.Unknown),
      timeout)
  }

  test("Matching a species should return a JSON containing the species name") {
    for species <- speciesExamples do
      val response: JSON = Await.result(
        GbifService(endpoint, maxAttempts).matchSpecies(species),
        timeout)
      assert(response.contains(species.latinName))
  }

  test("Matching the same species should return the same JSON") {
    for species <- speciesExamples do
      val response1: JSON = Await.result(
        GbifService(endpoint, maxAttempts).matchSpecies(species),
        timeout)
      val response2: JSON = Await.result(
        GbifService(endpoint, maxAttempts).matchSpecies(species),
        timeout)
      assert(response1 == response2)
  }

  test("Matching different species should return different JSON") {
    var responses = scala.collection.mutable.HashMap.empty[String, JSON]
    for species <- speciesExamples do
      val json: JSON = Await.result(
        GbifService(endpoint, maxAttempts).matchSpecies(species),
        timeout)
      responses += (species.latinName -> json)
    for responseA <- responses do
      for responseB <- responses do
        if responseA._1 != responseB._1 then // species names are different
          assert(responseA._2 != responseB._2) // content is different
  }

  test("Matching a list of species should return a list of the same length") {
    val matchedSpecies = Await.result(GbifService(endpoint, maxAttempts).matchSpeciesList(speciesExamples), timeout)
    assert(speciesExamples.length == matchedSpecies.length)
  }