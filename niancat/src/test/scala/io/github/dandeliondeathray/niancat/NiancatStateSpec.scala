package io.github.dandeliondeathray.niancat

import org.scalatest._
import org.scalamock.scalatest._
import org.scalactic.NormMethods._
import StringNormalizer._
import WordNormalizer._
import PuzzleNormalizer._

class NiancatStateSpec extends FlatSpec with Matchers with MockFactory {
  "A default state" should "not have a result set" in {
    val state = new NiancatState()
    state.result(Seq()) shouldBe None
  }

  it should "store the new puzzle" in {
    val state = new NiancatState()
    state.reset(Puzzle("VANTRIVSA"), true)
    state.puzzle shouldBe Some(Puzzle("VANTRIVSA"))
  }

  it should "normalize the puzzle when storing it" in {
    val state = new NiancatState()
    val puzzle = Puzzle("pikétröja")
    state.reset(puzzle, true)
    state.puzzle shouldBe Some(puzzle.norm)
  }

  "A state with a puzzle set" should "store a solution" in {
    val state = new NiancatState()
    state.reset(Puzzle("TRIVASVAN"), true) // VANTRIVAS

    state.solved(User("foo"), Word("VANTRIVAS"), true)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe Some(
      SolutionResult(Map(Word("VANTRIVAS") -> Seq(User("foo"))), Map(User("foo") -> 1))
    )
  }

  it should "not increase streak on weekends" in {
    val state = new NiancatState()
    state.reset(Puzzle("TRIVASVAN"), true) // VANTRIVAS

    state.solved(User("foo"), Word("VANTRIVAS"), false)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe Some(
      SolutionResult(Map(Word("VANTRIVAS") -> Seq(User("foo"))), Map())
    )
  }

  it should "return solutions in the order in which they are found" in {
    val state = new NiancatState()
    state.reset(Puzzle("TRIVASVAN"), true) // VANTRIVAS

    state.solved(User("foo"), Word("VANTRIVAS"), true)
    state.solved(User("bar"), Word("VANTRIVAS"), true)
    state.solved(User("baz"), Word("VANTRIVAS"), true)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe
      Some(
        SolutionResult(
          Map(Word("VANTRIVAS") -> Seq(User("foo"), User("bar"), User("baz"))),
          Map(User("foo") -> 1, User("bar") -> 1, User("baz") -> 1)
        )
      )
  }

  it should "only list one solution if a user solves the same word several times" in {
    val state = new NiancatState()
    state.reset(Puzzle("TRIVASVAN"), true) // VANTRIVAS

    state.solved(User("foo"), Word("VANTRIVAS"), true)
    state.solved(User("foo"), Word("VANTRIVAS"), true)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe Some(
      SolutionResult(Map(Word("VANTRIVAS") -> Seq(User("foo"))), Map(User("foo") -> 1))
    )
  }

  it should "forget solutions when a new puzzle is set" in {
    val state = new NiancatState()
    state.reset(Puzzle("GURKPUSSA"), true)
    state.solved(User("foo"), Word("PUSSGURKA"), true)
    state.solved(User("baz"), Word("PUSSGURKA"), true)

    state.reset(Puzzle("DATORLESP"), true)

    state.solved(User("foo"), Word("DATORSPEL"), true)
    state.solved(User("bar"), Word("DATORSPEL"), true)

    state.result(Seq(Word("DATORSPEL"), Word("SPELDATOR"), Word("REPSOLDAT"), Word("LEDARPOST"))) shouldBe Some(
      SolutionResult(
        Map(
          Word("DATORSPEL") -> Seq(User("foo"), User("bar")),
          Word("SPELDATOR") -> Seq(),
          Word("REPSOLDAT") -> Seq(),
          Word("LEDARPOST") -> Seq()
        ),
        Map(User("foo") -> 2, User("bar") -> 1, User("baz") -> 1)
      )
    )

    state.reset(Puzzle("VANTRIVAS"), true)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe Some(
      SolutionResult(
        Map(Word("VANTRIVAS") -> Seq()),
        Map(User("foo") -> 2, User("bar") -> 1)
      )
    )
  }

  it should "forget attempt counts when a new puzzle is set" in {
    val state = new NiancatState()
    state.reset(Puzzle("GURKPUSSA"), true)
    state.countAttempt(User("foo"), validAttempt = true)
    state.solved(User("foo"), Word("PUSSGURKA"), true)
    state.countAttempt(User("baz"), validAttempt = false)
    state.countAttempt(User("baz"), validAttempt = false)
    state.countAttempt(User("baz"), validAttempt = true)
    state.solved(User("baz"), Word("PUSSGURKA"), true)
    state.countAttempt(User("boz"), validAttempt = false)

    state.userState(User("foo")) shouldBe UserState(validAttempts = 1, invalidAttempts = 0)
    state.userState(User("baz")) shouldBe UserState(validAttempts = 1, invalidAttempts = 2)
    state.userState(User("boz")) shouldBe UserState(validAttempts = 0, invalidAttempts = 1)

    state.reset(Puzzle("DATORLESP"), true)

    state.userState(User("foo")) shouldBe UserState(validAttempts = 0, invalidAttempts = 0)
    state.userState(User("baz")) shouldBe UserState(validAttempts = 0, invalidAttempts = 0)
    state.userState(User("boz")) shouldBe UserState(validAttempts = 0, invalidAttempts = 0)
  }

  it should "not forget streaks when new puzzle is set on weekends" in {
    val state = new NiancatState()
    state.reset(Puzzle("GURKPUSSA"), true)
    state.solved(User("foo"), Word("PUSSGURKA"), true)
    state.solved(User("baz"), Word("PUSSGURKA"), true)

    state.reset(Puzzle("DATORLESP"), true)

    state.solved(User("foo"), Word("DATORSPEL"), true)
    state.solved(User("bar"), Word("DATORSPEL"), true)

    state.result(Seq(Word("DATORSPEL"), Word("SPELDATOR"), Word("REPSOLDAT"), Word("LEDARPOST"))) shouldBe Some(
      SolutionResult(
        Map(
          Word("DATORSPEL") -> Seq(User("foo"), User("bar")),
          Word("SPELDATOR") -> Seq(),
          Word("REPSOLDAT") -> Seq(),
          Word("LEDARPOST") -> Seq()
        ),
        Map(User("foo") -> 2, User("bar") -> 1, User("baz") -> 1)
      )
    )

    state.reset(Puzzle("VANTRIVAS"), false)

    state.result(Seq(Word("VANTRIVAS"))) shouldBe Some(
      SolutionResult(
        Map(Word("VANTRIVAS") -> Seq()),
        Map(User("foo") -> 2, User("bar") -> 1, User("baz") -> 1)
      )
    )
  }

  it should "normalize the puzzle on reset" in {
    val state = new NiancatState()
    val puzzle = Puzzle("piketröja")

    state.reset(puzzle, true)

    state.puzzle shouldBe Some(puzzle.norm)
  }

  it should "normalize words that users solve" in {
    val state = new NiancatState()

    state.reset(Puzzle("PIKÉTRÖJA"), true)
    val word = Word("pikétröja")
    state.solved(User("foo"), word, true)

    state.result(Seq(Word("PIKÉTRÖJA").norm)) shouldBe Some(
      SolutionResult(Map(word.norm -> Seq(User("foo"))), Map(User("foo") -> 1))
    )
  }

  "a state with several solutions" should "return all of them" in {
    val state = new NiancatState()
    state.reset(Puzzle("DATORLESP"), true) // DATORSPEL, SPELDATOR, LEDARPOST, REPSOLDAT

    state.solved(User("foo"), Word("DATORSPEL"), true)
    state.solved(User("bar"), Word("DATORSPEL"), true)
    state.solved(User("foo"), Word("SPELDATOR"), true)
    state.solved(User("baz"), Word("LEDARPOST"), true)

    state.result(Seq(Word("DATORSPEL"), Word("SPELDATOR"), Word("REPSOLDAT"), Word("LEDARPOST"))) shouldBe Some(
      SolutionResult(
        Map(
          Word("DATORSPEL") -> Seq(User("foo"), User("bar")),
          Word("SPELDATOR") -> Seq(User("foo")),
          Word("LEDARPOST") -> Seq(User("baz")),
          Word("REPSOLDAT") -> Seq()
        ),
        Map(User("foo") -> 2, User("bar") -> 1, User("baz") -> 1)
      )
    )
  }

  it should "forget any unconfirmed unsolution when setting an unsolution for the same user" in {
    val state = new NiancatState()

    state.reset(Puzzle("VIVANSART"), true)
    state.storeUnconfirmedUnsolution(User("foo"), "an unconfirmed unsolution")

    state.storeUnsolution(User("foo"), Puzzle("VIVANSART").letters)

    val stored = state.unconfirmedUnsolutions() get User("foo")

    stored shouldBe None
  }

  it should "list unsolutions in the order they were added" in {
    val state = new NiancatState()
    val puzzle = Puzzle("VIVANSART")
    state.reset(puzzle, true)

    state.storeUnsolution(User("foo"), s"${puzzle.letters} 1")
    state.storeUnsolution(User("foo"), s"${puzzle.letters} 2")

    state.unsolutions() shouldBe Map(User("foo") -> Seq(s"${puzzle.letters} 1", s"${puzzle.letters} 2"))
  }
}
