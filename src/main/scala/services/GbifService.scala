package services

import com.typesafe.config.ConfigFactory
import models.{Binomial, Species}
import models.Types.JSON
import requests.Response

import java.net.{HttpRetryException, URLEncoder}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import java.io.IOException
import scala.util.control.NonFatal

/**
 * GBIF API client
 * Implements TaxonomyService trait
 *
 * @see https://www.gbif.org/developer/species
 * @param endpoint URL of the API (set in conf/application.conf)
 */
class GbifService(endpoint: String, maxAttempts: Int) extends TaxonomyService:
  /**
   * @param name latin name of a species
   * @return JSON string
   */
  override def matchSpecies(species: Species): Future[JSON] =
    def attempt(remainingAttempts: Int): Future[JSON] =
      if remainingAttempts == 0 then Future.failed(IOException("Failed after $maxAttempts"))
      else
        getApiResponse(species.latinName).recoverWith { // retry if failed
          case NonFatal(_) =>
            attempt(remainingAttempts - 1)
        }
    attempt(maxAttempts)

  /**
   * @param name list of latin names of species
   * @return list of JSON strings
   */
  override def matchSpeciesList(speciesList: Seq[Species]): Future[Seq[JSON]] =
    val emptyList: Future[Seq[JSON]] = Future.successful(Seq.empty[JSON])
    speciesList.foldLeft(emptyList) {
      (futureSpecies, species) =>
        futureSpecies.zip(matchSpecies(species)).map((sps, sp) => sps :+ sp)
    }

  /**
   * Send a request for a species name to the GBIF API
   *
   * @param name
   * @return JSON string
   */
  protected def getApiResponse(name: Binomial): Future[JSON] =
    Future {
      // request parameters sent to the API
      val params = Map(
        "verbose" -> "true",
        "kingdom" -> "Animalia", // only search for animals (to avoid confusion as some plants and animals have the same latin name)
        "name" -> name.toString // the latin name requested
      )
      val response = requests.get(endpoint, params = params)
      response.text()
    }
