package io.github.dandeliondeathray.niancat

import org.scalatest._
import matchers._
import org.scalamock.scalatest._
import scala.reflect._

import org.scalactic.NormMethods._
import StringNormalizer._
import PuzzleNormalizer._

trait ResponseMatchers {
  class ContainsResponseMatcher(expectedResponse: Response) extends Matcher[Response] {
    def apply(response: Response) = {
      val hasOrIsExpectedResponse = response match {
        case CompositeResponse(responses: Vector[Response]) => responses contains expectedResponse
        case r: Response                                    => r == expectedResponse
      }
      MatchResult(
        hasOrIsExpectedResponse,
        s"""Response $response did not contain expected response $expectedResponse""",
        s"""Response $response contained expected response $expectedResponse"""
      )
    }
  }

  class ContainsResponseTypeMatcher(responseType: Class[_]) extends Matcher[Response] {
    def apply(response: Response) = {
      val hasOrIsExpectedType = response match {
        case CompositeResponse(responses: Vector[Response]) => {
          responses exists (responseType == _.getClass)
        }
        case r: Response => responseType == r.getClass
      }

      MatchResult(
        hasOrIsExpectedType,
        s"""Response $response did not contain a response of type $responseType""",
        s"""Response $response did contain a response of type $responseType"""
      )
    }
  }

  def containResponse(expectedResponse: Response) = new ContainsResponseMatcher(expectedResponse)
  def containResponseOfType(responseType: Class[_]) = new ContainsResponseTypeMatcher(responseType)
}

/**
  * Created by Erik Edin on 2017-05-01.
  */
class PuzzleEngineSpec extends FlatSpec with Matchers with MockFactory with ResponseMatchers {
  val defaultPuzzle = Puzzle("VIVANSART")
  val shuffledPuzzle = Puzzle("ARTSVIVAN")
  val defaultWord = Word("VANTRIVAS")

  def defaultSolutionResult = SolutionResult()

  def acceptingDictionary: Dictionary = {
    val dictionary = stub[Dictionary]
    (dictionary.has _) when (*) returns (true) anyNumberOfTimes ()
    (dictionary.solutions _) when (*) returns (Seq(Word("VANTRIVAS"))) anyNumberOfTimes ()
    (dictionary.solutionId _) when (*, *) returns Some(1) anyNumberOfTimes ()
    dictionary
  }

  def rejectingDictionary: Dictionary = {
    val dictionary = stub[Dictionary]
    (dictionary.has _) when (*) returns (false) anyNumberOfTimes ()
    (dictionary.solutions _) when (*) returns Seq() anyNumberOfTimes ()
    (dictionary.solutionId _) when (*, *) returns None anyNumberOfTimes ()
    dictionary
  }

  def acceptingMultipleSolutionsDictionary: Dictionary = {
    val dictionary = stub[Dictionary]
    (dictionary.has _) when (*) returns (true) anyNumberOfTimes ()
    (dictionary.solutions _) when (*) returns (Seq(
      Word("SPELDATOR"),
      Word("DATORSPEL"),
      Word("REPSOLDAT"),
      Word("LEDARPOST")
    )) anyNumberOfTimes ()
    (dictionary.solutionId _) when (*, *) returns (Some(1)) anyNumberOfTimes ()
    dictionary
  }

  trait EmptyState extends State {
    def puzzle(): Option[Puzzle] = None
    def result(allSolutions: Seq[Word]): Option[SolutionResult] = None
    def reset(p: Puzzle, isWeekday: Boolean): Unit = ()
    def solved(user: User, word: Word, isWeekday: Boolean): Unit = ()
    def storeUnconfirmedUnsolution(user: User, text: String): Unit = ()
    def storeUnsolution(user: User, text: String): Unit = ()
    def streak(user: User): Int = 0
    def streaks(): Map[User, Int] = Map()
    def unsolutions(): Map[User, Seq[String]] = Map()
    def unconfirmedUnsolutions(): Map[User, String] = Map()
    def hasSolved(user: User, word: Word): Boolean = false
  }

  val emptyState: State = new EmptyState() {}

  def engineWithPuzzle(p: Puzzle, dictionary: Dictionary = acceptingDictionary): PuzzleEngine = {
    val state = new NiancatState(new InMemoryState())
    state.reset(p, true)
    new PuzzleEngine(state, dictionary)
  }

  "An engine with no puzzle set" should "reply that no puzzle is set, when asked for the puzzle" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)
    val response = Get()(engine)
    response shouldBe NoPuzzleSet()
  }

  it should "notify that a new puzzle is set" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)
    val response = SetPuzzle(defaultPuzzle, true)(engine)

    response should containResponse(NewPuzzle(defaultPuzzle))
  }

  it should "normalize the puzzle when notifying that a new one is set" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)
    val puzzle = Puzzle("pikétröja")
    val response = SetPuzzle(puzzle, true)(engine)

    response should containResponse(NewPuzzle(puzzle.norm))
  }

  it should "reply that no puzzle is set, when a user checks a solution" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)

    response shouldBe NoPuzzleSet()
  }

  it should "not reply with a previous puzzle when a new one is set" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)

    val response = SetPuzzle(defaultPuzzle, true)(engine)

    response should not(containResponseOfType(classTag[YesterdaysPuzzle].runtimeClass))
  }

  it should "reply that no puzzle is set if a user tries to store an unsolution" in {
    val engine = new PuzzleEngine(emptyState, acceptingDictionary)

    val response = AddUnsolution("Some unsolution", User("foo"))(engine)

    response shouldBe NoPuzzleSet()
  }

  "An engine with a puzzle set" should "reply with the puzzle, when asked for the puzzle" in {
    val engine = engineWithPuzzle(defaultPuzzle)
    val response = Get()(engine)
    response shouldBe GetReply(defaultPuzzle)
  }

  it should "reply that a word is not in the dictionary, when a user checks a word" in {
    val engine = engineWithPuzzle(defaultPuzzle, rejectingDictionary)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)

    response shouldBe NotInTheDictionary(defaultWord)
  }

  it should "reply that a word is correct, if the word is in the dictionary" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)

    response should containResponse(CorrectSolution(defaultWord))
  }

  it should "reply with a mismatch if the word does not match the puzzle" in {
    val engine = engineWithPuzzle(defaultPuzzle, rejectingDictionary)

    val mismatchingWord = Word("NOTRIGHTX")
    val response = CheckSolution(mismatchingWord, User("foo"), true)(engine)

    response shouldBe a[WordAndPuzzleMismatch]
  }

  it should "show the users the mismatching letters" in {
    val puzzle = Puzzle("ABCDEFGHI")
    val mismatchingWord = Word("DEFGHIJKL")
    val engine = engineWithPuzzle(puzzle)

    val response = CheckSolution(mismatchingWord, User("foo"), true)(engine)

    response should have('tooMany ("JKL"), 'tooFew ("ABC"))
  }

  it should "repeat mismatching letters the correct number of times" in {
    val puzzle = Puzzle("ABCDEFGHI")
    val mismatchingWord = Word("AAADEFGHI")
    val engine = engineWithPuzzle(puzzle)

    val response = CheckSolution(mismatchingWord, User("foo"), true)(engine)

    response should have('tooMany ("AA"), 'tooFew ("BC"))
  }

  it should "reply with mismatch if the word is not nine letters long" in {
    val puzzle = Puzzle("ABCDEFGHI")
    val engine = engineWithPuzzle(puzzle, rejectingDictionary)

    val wordIsWrongLength = Word("ABCDEF")
    val response = CheckSolution(wordIsWrongLength, User("foo"), true)(engine)

    response shouldBe IncorrectLength(wordIsWrongLength, None, Some("GHI"))
  }

  it should "notify the channel about yesterdays puzzle when a new one is set" in {
    // Return defaultWord as the solution to defaultPuzzle
    val state = stub[State]
    (state.puzzle _) when () returns None anyNumberOfTimes ()
    (state.result _) when (*) returns (Some(defaultSolutionResult)) anyNumberOfTimes ()
    (state.unsolutions _) when () returns Map()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = SetPuzzle(Puzzle("ABCDEFGHI"), true)(engine)

    response should containResponseOfType(classTag[YesterdaysPuzzle].runtimeClass)
  }

  it should "not include the new puzzles solution when it is set" in {
    val dictionary = stub[Dictionary]
    (dictionary.has _) when (*) returns (true) anyNumberOfTimes ()
    (dictionary.solutions _) when (Puzzle("DATORSPLE")) returns (Seq(
      Word("DATORSPEL")
    )) anyNumberOfTimes ()
    (dictionary.solutions _) when (Puzzle("VANTRIVSA")) returns (Seq(
      Word("VANTRIVAS")
    )) anyNumberOfTimes ()
    (dictionary.solutionId _) when (*, *) returns (Some(1)) anyNumberOfTimes ()

    val state = new NiancatState(new InMemoryState())
    val engine = new PuzzleEngine(state, dictionary)

    SetPuzzle(Puzzle("DATORSPLE"), true)(engine)
    val response = SetPuzzle(Puzzle("VANTRIVSA"), true)(engine).asInstanceOf[CompositeResponse]

    val maybeYesterdaysPuzzle: Option[YesterdaysPuzzle] = response.responses.collectFirst({
      case x: YesterdaysPuzzle => x
    })

    maybeYesterdaysPuzzle match {
      case Some(YesterdaysPuzzle(solutionResult)) =>
        assert(!solutionResult.wordsAndSolvers.contains(Word("VANTRIVAS")))
      case None =>
        fail("Response had no YesterdaysPuzzle")
    }
  }

  it should "reset the puzzle solution when a new one is set" in {
    val newPuzzle = Puzzle("ABCDEFGHI")

    val state = mock[State]
    (state.puzzle _) expects () returning None anyNumberOfTimes ()
    (state.result _) expects (*) returning (Some(defaultSolutionResult)) anyNumberOfTimes ()
    (state.reset _) expects (newPuzzle, true)
    (state.unsolutions _) expects () returning Map() anyNumberOfTimes ()
    (state.streaks _) expects () returning Map() anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    SetPuzzle(newPuzzle, true)(engine)
  }

  it should "respond that a puzzle is invalid if there are no solutions for it" in {
    val invalidPuzzle = Puzzle("ABCDEFGHI")

    val engine = new PuzzleEngine(emptyState, rejectingDictionary)

    val response = SetPuzzle(invalidPuzzle, true)(engine)

    response shouldBe InvalidPuzzle(invalidPuzzle)
  }

  it should "not reset the puzzle if the new puzzle is invalid" in {
    val invalidPuzzle = Puzzle("ABCDEFGHI")

    val state = mock[State]
    (state.puzzle _) expects () returning None anyNumberOfTimes ()
    (state.reset _) expects (invalidPuzzle, true) never ()

    val engine = new PuzzleEngine(state, rejectingDictionary)

    SetPuzzle(invalidPuzzle, true)(engine)
  }

  it should "let the users know if a puzzle has more than one solution" in {
    val dictionary = acceptingMultipleSolutionsDictionary
    val newPuzzle = Puzzle("ABCDEFGHI")

    val state = stub[State]
    (state.puzzle _) when () returns Some(Puzzle("DATORSPLE")) anyNumberOfTimes ()
    (state.unsolutions _) when () returns Map() anyNumberOfTimes ()
    (state.result _) when (*) returns None
    (state.reset _) when (*, *) anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, dictionary)

    val response = SetPuzzle(newPuzzle, true)(engine)

    response should containResponse(MultipleSolutions(4))
  }

  it should "not mention multiple solutions if there is only one" in {
    val newPuzzle = Puzzle("ABCDEFGHI")

    val engine = engineWithPuzzle(Puzzle("VANTRIVAS"), acceptingDictionary)

    val response = SetPuzzle(newPuzzle, true)(engine)

    response should not(containResponseOfType(classTag[MultipleSolutions].runtimeClass))
  }

  it should "store a solution" in {
    val state = mock[State]
    (state.puzzle _) expects () returning Some(defaultPuzzle) anyNumberOfTimes ()
    (state.solved _) expects (User("foo"), defaultWord, true)
    (state.streak _) expects (User("foo")) returns (1) anyNumberOfTimes ()
    (state.hasSolved _) expects (User("foo"), defaultWord) returning (false) anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)
  }

  it should "not store an invalid solution" in {
    val state = mock[State]
    (state.puzzle _) expects () returning Some(defaultPuzzle) anyNumberOfTimes ()
    (state.solved _) expects (User("foo"), defaultWord, true) never ()

    val engine = new PuzzleEngine(state, rejectingDictionary)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)
  }

  it should "not reset the puzzle if the new puzzle is the same as the old" in {
    val state = mock[State]
    (state.puzzle _) expects () returning Some(defaultPuzzle) anyNumberOfTimes ()
    (state.reset _) expects (*, *) never ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    SetPuzzle(defaultPuzzle, true)(engine)
  }

  it should "respond with SamePuzzle if the new puzzle is an anagram of the old" in {
    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle) anyNumberOfTimes ()
    (state.reset _) when (*, *) anyNumberOfTimes ()
    (state.result _) when (*) returns (Some(SolutionResult())) anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = SetPuzzle(shuffledPuzzle, true)(engine)

    response shouldBe SamePuzzle(shuffledPuzzle)
  }

  it should "respond with SamePuzzle if the new puzzle is the same as the old" in {
    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle) anyNumberOfTimes ()
    (state.reset _) when (*, *) anyNumberOfTimes ()
    (state.result _) when (*) returns (Some(SolutionResult())) anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = SetPuzzle(defaultPuzzle, true)(engine)

    response shouldBe SamePuzzle(defaultPuzzle)
  }

  it should "normalize the puzzle when seeing if it's the same one" in {
    val puzzle = Puzzle("PIKETRÖJA")

    val state = stub[State]
    (state.puzzle _) when () returns Some(puzzle) anyNumberOfTimes ()
    (state.reset _) when (*, *) anyNumberOfTimes ()
    (state.result _) when (*) returns (Some(SolutionResult())) anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = SetPuzzle(Puzzle("pikétröja"), true)(engine)

    response shouldBe SamePuzzle(puzzle)
    (state.reset _) verify (*, *) never ()
  }

  it should "notify the main channel if a user solves the puzzle" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)

    response should containResponse(SolutionNotification(User("foo"), 1, None))
  }

  it should "notify the main channel if a user solves the puzzle on the weekend" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = CheckSolution(defaultWord, User("foo"), false)(engine)

    response should containResponse(SolutionNotification(User("foo"), 0, None))
  }

  it should "notify with a streak of two if the user has previously solved exactly one puzzle" in {
    val state = new NiancatState(new InMemoryState())
    state.reset(defaultPuzzle, true)
    val engine = new PuzzleEngine(state, acceptingDictionary)

    CheckSolution(defaultWord, User("foo"), true)(engine)

    state.reset(Puzzle("DATORSPLE"), true)
    val response = CheckSolution(Word("DATORSPEL"), User("foo"), true)(engine)

    response should containResponse(SolutionNotification(User("foo"), 2, None))
  }

  it should "not notify the main channel if a user solves the puzzle again" in {
    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle) anyNumberOfTimes ()
    (state.result _) when (*) returns (None) anyNumberOfTimes ()
    (state.reset _) when (*, *) anyNumberOfTimes ()
    (state.solved _) when (*, *, *) anyNumberOfTimes ()
    inSequence {
      (state.hasSolved _) when (*, *) returns (false) once ()
      (state.hasSolved _) when (*, *) returns (true) anyNumberOfTimes ()
    } anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    CheckSolution(defaultWord, User("foo"), true)(engine)

    val secondResponse = CheckSolution(defaultWord, User("foo"), true)(engine)

    secondResponse shouldBe CorrectSolution(defaultWord)
  }

  it should "not include solution id in the solution notification is there is only one solution" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = CheckSolution(defaultWord, User("foo"), true)(engine)

    response should containResponse(SolutionNotification(User("foo"), 1, None))
  }

  it should "include the solution id if there are multiple solutions" in {
    val solutionId = 3
    val dictionary = stub[Dictionary]
    (dictionary.solutions _) when (*) returns (Seq(
      Word("DATORSPEL"),
      Word("SPELDATOR"),
      Word("LEDARPOST"),
      Word("REPSOLDAT")
    )) anyNumberOfTimes ()
    (dictionary.solutionId _) when (*, *) returns (Some(solutionId)) anyNumberOfTimes ()
    (dictionary.has _) when (*) returns true

    val state = stub[State]
    (state.result _) when (*) returns (Some(defaultSolutionResult)) anyNumberOfTimes ()
    (state.reset _) when (*, *) anyNumberOfTimes ()

    val engine = engineWithPuzzle(Puzzle("DATORSPLE"), dictionary)

    val response = CheckSolution(Word("DATORSPEL"), User("foo"), true)(engine)

    response should containResponse(SolutionNotification(User("foo"), 1, Some(solutionId)))
  }

  it should "not send a response when adding an unsolution" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = AddUnsolution(s"Some ${defaultPuzzle.letters}", User("foo"))(engine)

    response shouldBe UnsolutionAdded()
  }

  it should "separate unsolutions based on users" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    AddUnsolution(s"Some ${defaultPuzzle.letters}", User("foo"))(engine)
    val response = ListUnsolutions(User("otheruser"))(engine)

    response shouldBe NoUnsolutions()
  }

  it should "clear all unsolutions when a new puzzle is set" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    AddUnsolution(s"Some ${defaultPuzzle.letters}", User("foo"))(engine)
    SetPuzzle(Puzzle("DATORSPEL"), true)(engine)

    val response = ListUnsolutions(User("foo"))(engine)

    response shouldBe NoUnsolutions()
  }

  it should "reply with all unsolutions if a new puzzle is set" in {
    val textFoo1 = s"Some ${defaultPuzzle.letters}"
    val textFoo2 = s"Some other ${defaultPuzzle.letters}"
    val textBar1 = s"Some ${defaultPuzzle.letters} for another user"

    val engine = new PuzzleEngine(new State with EmptyState {
      override def puzzle() = Some(defaultPuzzle)
      override def unsolutions() = Map(User("foo") -> List(textFoo1, textFoo2), User("bar") -> List(textBar1))
    }, acceptingDictionary)

    val response = SetPuzzle(Puzzle("DATORSPEL"), true)(engine)

    response should containResponse(
      AllUnsolutions(Map(User("foo") -> List(textFoo1, textFoo2), User("bar") -> List(textBar1)))
    )
  }

  it should "not mention unsolutions when a new puzzle is set, if there are no unsolutions" in {
    val engine = engineWithPuzzle(defaultPuzzle)

    val response = SetPuzzle(Puzzle("DATORSPEL"), true)(engine)

    response shouldNot containResponseOfType(classTag[AllUnsolutions].runtimeClass)
  }

  it should "reply with unconfirmed unsolutions if no word matches the puzzle" in {
    val engine = engineWithPuzzle(defaultPuzzle)
    val unsolutionText = s"No word matches the puzzle"

    val response = AddUnsolution(unsolutionText, User("foo"))(engine)

    response shouldBe UnsolutionNeedsConfirmation(defaultPuzzle)
  }

  it should "save an unconfirmed unsolutions if it's saved a second time" in {
    val unsolutionText = "No word matches the puzzle"
    val user = User("foo")

    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle)
    (state.unconfirmedUnsolutions _) when () returns Map(user -> unsolutionText) once ()
    (state.storeUnsolution _) when (user, unsolutionText) returning () once ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = AddUnsolution(unsolutionText, User("foo"))(engine)

    response shouldBe UnsolutionAdded()
  }

  it should "state that there is no unsolution to confirm for an emty unsolution command" in {
    val unsolutionText = "No word matches the puzzle"
    val user = User("foo")

    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle)
    (state.unconfirmedUnsolutions _) when () returns Map() anyNumberOfTimes ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = AddUnsolution("", User("foo"))(engine)

    response shouldBe NoUnsolutionToConfirm()

    (state.storeUnsolution _) verify (*, *) never ()
  }

  it should "save the unconfirmed unsolution in case you use the empty add unsolution command" in {
    val unsolutionText = "No word matches the puzzle"
    val user = User("foo")

    val state = stub[State]
    (state.puzzle _) when () returns Some(defaultPuzzle)
    (state.unconfirmedUnsolutions _) when () returns Map(user -> unsolutionText) once ()
    (state.storeUnsolution _) when (user, unsolutionText) returning () once ()

    val engine = new PuzzleEngine(state, acceptingDictionary)

    val response = AddUnsolution("", User("foo"))(engine)

    response shouldBe UnsolutionAdded()
    (state.storeUnsolution _) verify (user, unsolutionText) once ()
  }
}
