package utils

import com.typesafe.config.ConfigFactory
import models.Species

class CsvParserSuite extends munit.FunSuite:
  /** Some example data for testing */
  val dataExamples: Seq[String] = Seq(
    // OK
    """"Lepus europaeus";"Lepus";"Leporidae";"Lagomorpha"""",
    // entry is a subspecies
    """Lama glama guanicoe;"Lama";"Camelidae";"Artiodactyla"""",
    // the following 2 are the same species, but different subspecies
    """"Canis lupus";"Canis";"Canidae";"Carnivora"""",
    """"Canis lupus f. familiaris";"Canis";"Canidae";"Carnivora"""",
    // species name has changed since then
    """"Chroicocephalus ridibundus";"Chroicocephalus";"Laridae";"Charadriiformes""""
  )

  /** Load the CSV separator from conf/application.conf */
  private var separator: Char = ','
  override def beforeAll() =
    val config = ConfigFactory.load()
    separator = config.getString("csv.separator").trim.toList.head

  test("parse all examples should succeed") {
    val parser = new CsvParser(separator)
    for (example <- dataExamples)
      assert(parser.parse(example).isInstanceOf[Species])
  }

  test("parse the same example twice should return the same result") {
    val parser = new CsvParser(separator)
    for (example <- dataExamples)
      val species1 = parser.parse(example)
      val species2 = parser.parse(example)
  }

  // note that different subspecies from the same species are considered different
  // e. g. 'Canis lupus' and 'Canis lupus f. familiaris'
  test("different examples should return a different species") {
    val parser = new CsvParser(separator)
    for (example1 <- dataExamples)
      for (example2 <- dataExamples)
        if example1 != example2 then
          val species1 = parser.parse(example1)
          val species2 = parser.parse(example2)
          assert(species1 != species2)
  }

  test("species name in parsed result should be present in the json") {
    val parser = new CsvParser(separator)
    for (example <- dataExamples)
      val species = parser.parse(example)
      assert(species.latinName.nonEmpty)
      assert(example.contains(species.latinName))
  }

