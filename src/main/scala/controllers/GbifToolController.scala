package controllers

import models.{Binomial, Species, Summary}

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal
import com.typesafe.config.{Config, ConfigFactory}
import controllers.GbifToolController.{config, timeout}
import errors.SpeciesValidationError
import models.Types.{FilePath, ValidatedSpecies}
import services.{GbifService, SpeciesCsvSink, SpeciesCsvSource, SpeciesSink, SpeciesSource, TaxonomyService}
import utils.{GbifParser, Report, ValidatingSpeciesParser}

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, Future}

/**
 * Entry point of the command-line application
 * see README for examples
 */
object GbifToolController:
  /** command-line commands */
  protected val acceptedCommands: List[String] = List("matchName", "matchAll", "ignore", "report")

  /** load the configuration from conf/application.conf */
  val config: Config = ConfigFactory.load()
  /** Debug mode read from conf/application.conf */
  val debug: Boolean = !config.hasPath("debug") | config.getBoolean("debug")
  /** max number of attempts to connect to the API */
  val maxAttempts: Int = config.getInt("maxAttempts")
  /** Separator chat used in CSV */
  val separator: Char = config.getString("csv.separator").toList.head
  /** max time to wait for each command */
  val timeout = Duration(config.getInt("timeout"), MILLISECONDS)

  /** Object for handling CSV files */
  val speciesSource: SpeciesSource = new SpeciesCsvSource(separator)
  val speciesSink: SpeciesSink = new SpeciesCsvSink(separator)

  /** Service used to call the external taxonomy reference API */
  protected val taxonomyService: TaxonomyService =
    new GbifService(config.getString("GBIF_URL_SPECIES_ENDPOINT"), maxAttempts)
  /** Parser used to parse the Json response from GBIF */
  protected val gbifParser: ValidatingSpeciesParser = new GbifParser()

  @main
  def main(params: String*): Unit =
    GbifToolController.route(params).onComplete {
      case Success(any) =>
        println(any)
      case Failure(ex) =>
        // if debug, print the exception
        if debug then throw ex
        // otherwise print message for end-user
        else println("An error occurred. See README for instructions.")
    }

  /**
   * Route according to command-line arguments
   * Fails immediately when the command-line arguments are not correct.
   *
   * @param params command-line parameters
   * @return Nothing
   */
  def route(params: Seq[String]): Future[Report] =
    // no command given, fail immediately
    if params.isEmpty then
      Future.failed(IllegalArgumentException("No arguments provided"))
    else
      val command = params.head // the command (as in acceptedCommands)
      val args = params.tail // the command arguments

      command match
        // matchName "species name"
        case command if command == "matchName" =>
          // matchName expects exactly one parameter, fail immediately otherwise
          if args.isEmpty | args.length != 1 then 
            Future.failed(IllegalArgumentException(s"Wrong number of parameters for matchName"))
          else
            Report.from(matchName(args.head))

        // matchAll inputfile.csv outputfile.csv
        case command if command == "matchAll" =>
          if args.isEmpty | args.length != 2 then
            Future.failed(IllegalArgumentException(s"Wrong number of parameters for matchAll"))
          else
            Report.from(matchAll(args.head, args.last))

        // unknown command detected, fail immediately
        case _ =>
          Future.failed(IllegalArgumentException(s"Expected one of $acceptedCommands, found $command"))

  /**
   * Take a single species name given as a command line parameter, match it to the GBIF API,
   * parse the response to a case class in the domain model.
   *
   * @param name The name of a species
   * @return a species object
   */
  def matchName(name: String): Future[ValidatedSpecies] =
    // taxonomyService is async, gbifParser is sync, so combine with .map
    val species = Species(name)
    taxonomyService.matchSpecies(species).map{ gbifResponse =>
      gbifParser.parse(gbifResponse)
    }


  /**
   * Load all the species from the input CSV file.
   * Check each one against the GBIF API.
   * Save the results to the output CSV file.
   *
   * @param inputPath path to input CSV
   * @param outputPath path to output CSV
   * @return a summary
   */
  def matchAll(inputPath: FilePath, outputPath: FilePath): Future[Summary] =
    val zippedLists: Future[Seq[(Species, ValidatedSpecies)]] =
      for
        speciesList <- speciesSource.listSpecies(inputPath)
        matchedJsonList <- taxonomyService.matchSpeciesList(speciesList)
        validatedSpeciesList = matchedJsonList.map{ gbifResponse => gbifParser.parse(gbifResponse)}
      yield
        speciesList.zip(validatedSpeciesList)

    // wait until completed
    val result = Await.result(zippedLists, timeout)

    // save the result, will throw an Exception if path can't be opened
    speciesSink.writeSpeciesList(result, outputPath)

    // return a summary, not the whole list
    Future.successful(Summary.fromSpeciesList(result))

