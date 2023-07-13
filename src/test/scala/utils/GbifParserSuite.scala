package utils

import errors.{SpeciesSynonymError, SpeciesUnknownError, SpeciesValidationError}
import models.Types.{JSON, ValidatedSpecies}
import models.{Species, Taxon, TaxonomicStatus}

class GbifParserSuite extends munit.FunSuite:
  /** Some example GBIF responses for testing */
  val gbifResponseExamples = Seq(
    // accepted
    """{"usageKey":2435099,"scientificName":"Puma concolor (Linnaeus, 1771)","canonicalName":"Puma concolor","rank":"SPECIES","status":"ACCEPTED","confidence":100,"note":"Similarity: name=110; authorship=0; classification=4; rank=6; status=1; singleMatch=5","matchType":"EXACT","kingdom":"Animalia","phylum":"Chordata","order":"Carnivora","family":"Felidae","genus":"Puma","species":"Puma concolor","kingdomKey":1,"phylumKey":44,"classKey":359,"orderKey":732,"familyKey":9703,"genusKey":2435098,"speciesKey":2435099,"synonym":false,"class":"Mammalia"}""",
    // synonym
    """{"usageKey":5217021,"acceptedUsageKey":2422872,"scientificName":"Bufo americanus Holbrook, 1836","canonicalName":"Bufo americanus","rank":"SPECIES","status":"SYNONYM","confidence":100,"note":"Similarity: name=110; authorship=0; classification=4; rank=6; status=0; nextMatch=5","matchType":"EXACT","alternatives":[{"usageKey":5217030,"acceptedUsageKey":2422888,"scientificName":"Bufo mexicanus Brocchi, 1879","canonicalName":"Bufo mexicanus","rank":"SPECIES","status":"SYNONYM","confidence":15,"note":"Similarity: name=5; authorship=0; classification=4; rank=6; status=0","matchType":"FUZZY","kingdom":"Animalia","phylum":"Chordata","order":"Anura","family":"Bufonidae","genus":"Anaxyrus","species":"Anaxyrus mexicanus","kingdomKey":1,"phylumKey":44,"classKey":131,"orderKey":952,"familyKey":6727,"genusKey":2422857,"speciesKey":2422888,"synonym":true,"class":"Amphibia"}],"kingdom":"Animalia","phylum":"Chordata","order":"Anura","family":"Bufonidae","genus":"Anaxyrus","species":"Anaxyrus americanus","kingdomKey":1,"phylumKey":44,"classKey":131,"orderKey":952,"familyKey":6727,"genusKey":2422857,"speciesKey":2422872,"synonym":true,"class":"Amphibia"}""",
    // unknown
    """{"usageKey":1,"scientificName":"Animalia","canonicalName":"Animalia","rank":"KINGDOM","status":"ACCEPTED","confidence":99,"note":"Similarity: name=100; classification=4; rank=12; status=1; nextMatch=5","matchType":"HIGHERRANK","kingdom":"Animalia","kingdomKey":1,"synonym":false}""",
    // accepted, but no 'order' (which is correct, turtles have no 'order')
    """{"usageKey":7696021,"scientificName":"Aldabrachelys gigantea (Schweigger, 1812)","canonicalName":"Aldabrachelys gigantea","rank":"SPECIES","status":"ACCEPTED","confidence":100,"note":"Similarity: name=110; authorship=0; classification=4; rank=6; status=1; singleMatch=5","matchType":"EXACT","kingdom":"Animalia","phylum":"Chordata","family":"Testudinidae","genus":"Aldabrachelys","species":"Aldabrachelys gigantea","kingdomKey":1,"phylumKey":44,"classKey":11418114,"familyKey":9618,"genusKey":2441821,"speciesKey":7696021,"synonym":false,"class":"Testudines"}""",
    // accepted
    """{"usageKey":1071240,"scientificName":"Anoplotrupes stercorosus (Hartmann, 1791)","canonicalName":"Anoplotrupes stercorosus","rank":"SPECIES","status":"ACCEPTED","confidence":99,"note":"Similarity: name=110; authorship=0; classification=4; rank=6; status=1; nextMatch=3","matchType":"EXACT","alternatives":[{"usageKey":12038775,"scientificName":"Anoplotrupes stercorosus (Scriba, 1791)","canonicalName":"Anoplotrupes stercorosus","rank":"SPECIES","status":"DOUBTFUL","confidence":97,"note":"Similarity: name=110; authorship=0; classification=4; rank=6; status=-5","matchType":"EXACT","kingdom":"Animalia","phylum":"Arthropoda","order":"Coleoptera","family":"Geotrupidae","genus":"Anoplotrupes","species":"Anoplotrupes stercorosus","kingdomKey":1,"phylumKey":54,"classKey":216,"orderKey":1470,"familyKey":8495,"genusKey":1071236,"speciesKey":12038775,"synonym":false,"class":"Insecta"}],"kingdom":"Animalia","phylum":"Arthropoda","order":"Coleoptera","family":"Geotrupidae","genus":"Anoplotrupes","species":"Anoplotrupes stercorosus","kingdomKey":1,"phylumKey":54,"classKey":216,"orderKey":1470,"familyKey":8495,"genusKey":1071236,"speciesKey":1071240,"synonym":false,"class":"Insecta"}"""
  )

  test("parse all examples should succeed") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      assert(parser.parse(example).isInstanceOf[ValidatedSpecies])
  }

  test("parse the same example twice should return the same result") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      val species1 = parser.parse(example)
      val species2 = parser.parse(example)
      assert(species1 == species2)
  }

  test("different examples should return a different species") {
    val parser = new GbifParser()
    for (example1 <- gbifResponseExamples)
      for (example2 <- gbifResponseExamples)
        if example1 != example2 then
          val species1 = parser.parse(example1)
          val species2 = parser.parse(example2)
          assert(species1 != species2)
  }

  test("species name in parsed result should be present in the json") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      parser.parse(example) match
        case Right(value) =>
          assert(example.contains(value.latinName.toString))
        case Left(ex) => // skip
  }

  test("GBIF id in parsed result should be present in the json") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      parser.parse(example) match
        case Right(species) =>
          assert(species.GbifUsageKey != 0)
          assert(example.contains(species.GbifUsageKey.toString))
        case Left(ex) =>  // skip
  }

  test("If a taxonomic rank is missing in the species object, e.g. turtles have no 'order', it should not be present in the json either") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      parser.parse(example) match
        case Right(species) =>
          if species.genus == Taxon.empty then assert(!example.contains("genus"))
          if species.familia == Taxon.empty then assert(!example.contains("family"))
          if species.ordo == Taxon.empty then assert(!example.contains("order"))
        case Left(ex) =>  // skip
  }

  test("Taxonomic status in the json should be ACCEPTED, except for UNKNOWN and CONFLICTING") {
    val parser = new GbifParser()
    for (example <- gbifResponseExamples)
      parser.parse(example) match
        // species is unknown, in that case GBIF may return "Animalia" with status ACCEPTED
        case Left(ex: SpeciesUnknownError) =>
          assert(
            example.contains(TaxonomicStatus.UNKNOWN.toString) | example.contains("Animalia")
          )
        case Left(ex: SpeciesSynonymError) =>
          assert(example.contains(TaxonomicStatus.SYNONYM.toString))
        case Right(species) => example.contains(TaxonomicStatus.ACCEPTED.toString)
  }

  test("Parsing the default species (Animalia, with GBIF key=1, status ACCEPTED) should return a validation error") {
    val unknownSpecies: JSON = """{"usageKey":1,"scientificName":"Animalia","canonicalName":"Animalia","rank":"KINGDOM","status":"ACCEPTED","confidence":99,"note":"Similarity: name=100; classification=4; rank=12; status=1; nextMatch=5","matchType":"HIGHERRANK","kingdom":"Animalia","kingdomKey":1,"synonym":false}"""
    val parser = new GbifParser()
    parser.parse(unknownSpecies) match
      case Right(response) => fail(s"Expected an error, got $response")
      case Left(ex) =>
        assert(ex.isInstanceOf[SpeciesValidationError])
  }
