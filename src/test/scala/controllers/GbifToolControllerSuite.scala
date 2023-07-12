package controllers

import errors.SpeciesValidationError
import models.Species

import java.io.{File, FileNotFoundException}
import scala.concurrent.{Await, Future}
import scala.util.Failure
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MILLISECONDS}

class GbifToolControllerSuite extends munit.FunSuite:
  /** max time to wait for each test */
  val timeout = Duration(10000, MILLISECONDS)

  /** The example input file */
  val inputPath = "src/test/data/input_small.csv"

  /** The example output file */
  val outputPath = "src/test/data/output.csv"

  // cleanup
  override def afterEach(context: AfterEach): Unit =
    // delete output file
    new File(outputPath).delete

  // when no command given, expect failure
  test("No command given") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq.empty), timeout)
    }
  }

  // when unknown command given, expect failure
  test("Unknown command requested") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq("not_a_command")), timeout)
    }
  }

  // matchName expects exactly one argument
  test("No argument given for matchName") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq("matchName")), timeout)
    }
  }
  test("Too many arguments given for matchName") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq("matchName", "one", "two")), timeout)
    }
  }
  test("Species names are two words between quotes") {
    Await.result(GbifToolController.route(Seq("matchName", "one two")), timeout) match
      case Right(response) => fail(s"Expected an error, got $response")
      case Left(ex) =>
        assert(ex.isInstanceOf[SpeciesValidationError])
  }

  // matchSpeciesFile expects exactly two arguments
  test("No argument given for matchAll") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq("matchAll")), timeout)
    }
  }
  test("Too many arguments given for matchSpeciesFile") {
    intercept[IllegalArgumentException] {
      Await.result(GbifToolController.route(Seq("matchAll", "one", "two", "three")), timeout)
    }
  }
  test("Attempting to process a non-existing file fails") {
    intercept[FileNotFoundException] {
      Await.result(GbifToolController.route(Seq("matchAll", "non_existing_inputfile.csv", "non_existing_outputfile.csv")), timeout)
    }
  }

  test("Processing an input file creates an output file") {
    var result = Await.result(GbifToolController.route(Seq("matchAll", inputPath, outputPath)), timeout)
    assert(File(outputPath).exists())
  }
