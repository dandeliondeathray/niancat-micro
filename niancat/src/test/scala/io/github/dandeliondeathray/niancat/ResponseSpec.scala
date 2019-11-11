package io.github.dandeliondeathray.niancat

import org.scalatest._

class ResponseSpec extends FlatSpec with Matchers {
  "NoPuzzleSet" should "say there's no puzzle set" in {
    NoPuzzleSet().toResponse should include("Nian är inte satt.")
  }

  "NotInTheDictionary" should "say a word isn't in the dictionary" in {
    NotInTheDictionary(Word("ABC")).toResponse should include("ABC finns inte med i SAOL")
  }

  "CorrectSolution" should "say a solution is korrekt" in {
    CorrectSolution(Word("DEF GHI")).toResponse should include("DEF GHI är korrekt")
  }

  "YesterdaysPuzzle" should "list all solutions and user names" in {
    val solutionResult = SolutionResult(
      Map(
        Word("ABCDEFGHI") -> Seq(User("foo"), User("bar"), User("qux")),
        Word("DEFGHIABC") -> Seq(User("baz"))
      )
    )

    val yesterdaysAsString = YesterdaysPuzzle(solutionResult).toResponse

    yesterdaysAsString should include("*ABCDEFGHI*: foo, bar, qux")
    yesterdaysAsString should include("*DEFGHIABC*: baz")
  }

  "Streaks" should "list all solvers and their streak count" in {
    val streaks = Map(
      User("foo") -> 1,
      User("oof") -> 1,
      User("bar") -> 3,
      User("baz") -> 2,
      User("qux") -> 2
    )

    val streaksAsString = Streaks(streaks).toResponse

    streaksAsString should include("3: bar")
    streaksAsString should include("2: qux, baz")
    streaksAsString should not include ("1:")
  }

  "MultipleSolutions" should "show the number of solutions" in {
    MultipleSolutions(3).toResponse should include("3")
  }

  "Solution notification" should "not include a solution id if not set" in {
    SolutionNotification(User("foo"), 1, None).toResponse should not(include("ord "))
  }

  it should "include id if set" in {
    SolutionNotification(User("foo"), 1, Some(42)).toResponse should include("ord 42")
  }

  it should "repeat :niancat: as many times as the current streak" in {
    SolutionNotification(User("foo"), 4, None).toResponse should include(":niancat: :niancat: :niancat: :niancat:")
  }

  "All unsolutions" should "contain the names of users and the texts" in {
    val text1 = "text1"
    val text2 = "text2"
    val text3 = "text3"

    val allUnsolutionsResponse =
      AllUnsolutions(Map(User("foo") -> List(text1, text2), User("bar") -> List(text3))).toResponse

    allUnsolutionsResponse should include("foo")
    allUnsolutionsResponse should include("bar")
    allUnsolutionsResponse should include(text1)
    allUnsolutionsResponse should include(text2)
    allUnsolutionsResponse should include(text3)
  }
}
