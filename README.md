Effective Programming in Scala --- Final Project
------------------------------------------------

## Project description

I work at the Natural History Museum in Berlin. 
The museum holds 30 million specimens of different types: fossils, bones, 
microscopic samples, genetic material etc. 
One of the essential ways these specimens are classified is through the biological taxonomy. 
Biological taxonomy is (perhaps surprisingly) subject to constant changes. 
Reasons for these constant changes include:
* As the genetic material of more and more species are sequenced, the relationships between species has to be revised.
* Depending on their speciality (paleontology, zoology, genetics...), different researchers might come to different conclusions regarding taxonomic classification.
* Personal preferences might lead some researchers to classify specimens differently (e.g. are dogs really domestic wolves, or are they a different species?).
* New species are discovered on a daily basis

Maintaining a complete and up-to-date taxonomy of the Animal kingdom - which consists of more than 4 million known species -
would be impossible, even for a large institution. Luckily, there are so-called "aggregators", that provide
a comprehensive reference taxonomy, compiled from trustworthy sources. One such aggregator is the Global Biodiversity Information Facility (GBIF). 
The [GBIF taxonomy](https://www.gbif.org/dataset/d7dddbf4-2cf0-4f39-9b2a-bb099caae36c) is accessible online 
and provides a [REST/JSON endpoint for querying it by species name](https://www.gbif.org/developer/species).
No API key is required to use the GBIF API.

This project shall provide a command-line application that checks a list of species names, 
i.e. latin binomial names, against the GBIF taxonomy. As researchers in natural history 
commonly work with "flat" tables (flat as in Excel), the input and the output of this application are CSV files.
The application shall provide a way to check for deprecated names, misspellings etc. and
report the results in human-readable format. It shall also provide for a way to bypass the check in certain cases, as 
researchers might occasionally disagree with GBIF.

Note: _In production, this project would be compiled to a jar, packed inside a Docker container with Java and 
could be integrated in a workflow for collection management._

## Usage examples
__Match name of species__

_matchName "species name"_

Example: open `sbt`, call `run matchName "Puma concolor"` (note that species name is between quotes)

Take a single species name (a latin name, i.e. binomial) given as a command line parameter, match it to the GBIF API, 
retry if necessary, parse the response to a class in the domain model, pretty-print the results.

__Match all__

_matchAll input_file_name.csv output_file-name.csv_

Example: I've included a taxonomy file of 100 species names extracted from historical sound recordings pre-1960. 
This file is in src/test/data/input.csv 
(actually, it's a ';'-separated file, csv import settings are in conf/application.conf).

open `sbt`, call `run matchAll src/test/data/input.csv src/test/data/output.csv`

Starting from a flat table (in csv format) of species names, genus, family and order,
match all species to the GBIF API, then save to disk a new csv table.
The output shall be:
* A new file containing a CSV table with species, genus, order, "taxonomic status" and GBIF-Id as returned by the GBIF API
* In case the match failed: the name of the species as in the original table, the reason for the failure as a "taxonomic status". The command `matchName`can then be used to further investigate the taxonomy of the species.
* A summary printed to the console: species counts by "taxonomic status".

For possible values of "taxonomic status" see below under "validation".

------------------------------------------------

This readme only contains a check-list to ease the evaluation process. Your
task is to create all the content of the project while respecting the 
requirements provided in the unit “Final Project Description”.

Please edit the content of the README to facilitate the evaluation work 
from the reviewers:

- Which build tool is used? Give the name of the build tool.
  * Build tool sbt, template used: `sbt new scala/scala-seed.g8` (the project is called "gbif-tool")
  * openjdk 11.0.0.1
  * Scala 3.3.0
  * Debian Linux


- Which third-party libraries are used? Give the names of the libraries, and 
  add links to the relevant parts of the build definition.
 
  These external libraries are configured in build.sbt (at the bottom of the file)
  * com.github.andyglow.typesafe-config-scala (loads the configuration in conf/application.conf in the main. I had to mark the conf dir as "Resources root" to get it to work in IntelliJ )
  * com.lihaoyi.requests (used by `src/main/scala/services/GbifService` to call the API)
  * com.typesafe.play.play-json (used by `src/main/scala/utils/GbifParser` to parse the JSON returned by the API)
 
  Additionally, from the Java standard library
  * java.net.URLEncoder (used by `src/main/scala/services/GbifService` to pre-process the data sent to the API)
  * java.io.BufferedSource and BufferedWriter (used by `src/main/scala/services/SpeciesCsvSource / SpeciesCsvSink` to read and write CSV files)
  * Some exceptions


- Where are the unit tests? List the unit test files.
  Unit test are in `src/main/test/scala`:
    * `GbifToolControllerSuite` tests command-line parameters
    * `GbifServiceSuite`, `GbifParserSuite` tests the requests to - and responses from - the API, and parsing the JSON received from the API
    * `SpeciesCsvSourceSuite`, `CsvParserSuite` test handling CSV files
    * `SummarySuite` test generating the summary from the output
    

- What is the domain model? Give links to the relevant Scala source files.
  The domain model is in `src/main/scala/models`
    * `Species` class models a species data as obtained from GBIF or from a CSV file, so the constructor is overloaded.
    * `TaxonomicStatus` is an enum used in Species to mark the status of this species check (ACCEPTED, SYNONYM, UNKNOWN...)
    * `Types` contains some custom types
    * `Summary` represents a summary of the data in a potentially large list of validated species

  
- What are the business operations? Give links to the Scala source files 
  that contain loops.
    * `src/main/scala/controllers/GbifToolController.matchSpeciesFile`contains a for-yield loop
    * `src/main/scala/services/GbifService.matchName` is iterative
    * `src/main/scala/services/GbifService.matchSpeciesList`, `src/main/scala/services/SpeciesCsvSource.listSpecies`, `src/main/scala/models/Summary.fromSpeciesList` contain for-do loops

     
- Which collections do you use? Give links to the relevant Scala source files.
  * `src/main/scala/controllers/GbifToolController.matchName` uses a map,
    `src/main/scala/controllers/GbifToolController.matchAll` uses a for-yield loop
  * `src/main/scala/services/SpeciesCsvSource.listSpecies` uses a mutable Seq (mutableSpeciesList), that is
    cast to an immutable set before being returned
  * `src/main/scala/services/GbifService.matchSpeciesList` uses a foldLeft
  * `Seq[String]`, `Seq[JSON]` and `Seq[ValidatedSpecies]` are used throughout the project


- What type of data validation do you do? Give links to the relevant Scala 
  source files.
 
  __Validation__ is implemented in `src/main/scala/utils/GbifParser.parse`. 
  There are many reasons why a species name might not be valid. The easiest way to cover them all is to 
  delegate validation to GBIF, and then to  examine the ["Taxonomic status" obtained from the GBIF response](https://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/TaxonomicStatus.html)

  __Data is considered valid if__
  * GBIF status is ACCEPTED and species, genus, family and order are identical to the GBIF response
  * GBIF status is ACCEPTED, but taxonomy is different, e.g. has changed since last call to GBIF, in that case, the new taxonomy shall overwrite the old one without asking
  * GBIF status DOUBTFUL is treated as accepted

  __Data is considered invalid if__
  * GBIF status is UNKNOWN: not found in GBIF, misspelled or newly discovered species, results in a `errors.SpeciesUnknownError`.
  * GBIF status is SYNONYM: Species name was deprecated in favor of a new name. The new name is in the JSON response. Results in a `errors.SpeciesSynonymError`.
  * GBIF status is MISAPPLIED, HOMOTYPIC_SYNONYM or PROPARTE_SYNONYM are treated as SYNONYM


- How do you report input validation errors? Give links to the relevant 
  Scala source files.
  
  * Validated responses are of type `ValidatedSpecies`, which is an alias for `Either[SpeciesValidationError, Species]`, defined in `src/main/scala/models/Types`. `SpeciesValidationError` and its subclasses are defined in `src/main/scala/errors/SpeciesValidationError`
  * After calling the command `matchAll` (see above for examples), 
      * A summary of validation errors is printed to the console by `models.Summary.fromSpeciesList`
      * Individual validation errors are stored in the output file generated by `src/main/scala/services/SpeciesCsvSink`


- How do you handle the other types of errors? Give links to the relevant 
  Scala source files.
  * This is a command-line application. Wrong command-line parameters are handled by `src/main/scala/controllers/GbifToolController.route`, 
    which returns a Future.failed containing an IllegalArgumentException.
    If the command or corresponding arguments are wrong, the execution is interrupted immediately.
  * Depending on the value of "debug" (set in conf/application.conf), non-fatal errors are reported on the terminal 
    as a short error message or as a complete stack trace. Error reporting is handled by `src/main/scala/controllers/GbifToolController.main`
  * Fatal runtime errors are not caught 
  
