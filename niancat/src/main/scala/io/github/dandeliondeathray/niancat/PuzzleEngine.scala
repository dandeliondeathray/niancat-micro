package io.github.dandeliondeathray.niancat

import org.scalactic._
import NormMethods._
import scala.collection.mutable
import StringNormalizer._
import PuzzleNormalizer._

/**
  * Created by Erik Edin on 2017-04-30.
  */
/** A Puzzle is a String of exactly nine characters. */
case class Puzzle(letters: String) {
  def matches(p: Puzzle): Boolean = {
    letters.norm.sorted == p.letters.norm.sorted
  }
}

/** A Word is a potential solution to a Puzzle, but can be any length. */
case class Word(letters: String) {
  def matches(p: Puzzle): Boolean = {
    letters.norm.sorted == p.letters.norm.sorted
  }
}

/** A User is of course a user in the chat room. */
case class User(name: String)

class PuzzleEngine(val state: State, val dictionary: Dictionary) {
  def findAllSolutions(p: Option[Puzzle]): Seq[Word] = {
    (p map (dictionary.solutions(_))) getOrElse Seq[Word]()
  }

  def set(nonNormalizedPuzzle: Puzzle, isWeekday: Boolean): Response = {
    val p = nonNormalizedPuzzle.norm
    if (state.puzzle.map(_ matches p) getOrElse false) {
      return SamePuzzle(p)
    }

    val allSolutions = dictionary.solutions(p)
    if (allSolutions.size == 0) {
      return InvalidPuzzle(p)
    }

    val maybeUnsolutions = if (state unsolutions () isEmpty) None else Some(state unsolutions ())

    val allYesterdaysSolutions = findAllSolutions(state.puzzle)

    val result = state.result(allYesterdaysSolutions)
    state.reset(p, isWeekday)
    val streaks = state.streaks()

    val responses: Vector[Option[Response]] = Vector(
      Some(NewPuzzle { p }),
      result map YesterdaysPuzzle,
      Some(Streaks(streaks)),
      Some(allSolutions.size) filter (_ > 1) map (MultipleSolutions(_)),
      maybeUnsolutions map (AllUnsolutions(_))
    )

    CompositeResponse(responses.flatten)
  }

  def get(): Response = {
    state.puzzle() match {
      case None            => NoPuzzleSet()
      case Some(p: Puzzle) => GetReply(p)
    }
  }

  def check(user: User, word: Word, isWeekday: Boolean): Response = {
    state.puzzle() match {
      case None            => NoPuzzleSet()
      case Some(p: Puzzle) => checkSolution(user, word, p, isWeekday)
    }
  }

  def addUnsolution(unsolution: String, user: User): Response = {
    if (state.puzzle().isEmpty) {
      return NoPuzzleSet()
    }

    if (!anyWordMatchesPuzzle(state.puzzle().get, unsolution)) {
      val unconfirmedUnsolution = state.unconfirmedUnsolutions().get(user)
      unconfirmedUnsolution match {
        case None if unsolution == "" => {
          // The user used an empty add unsolution command, but there's no unconfirmed unsolution.
          return NoUnsolutionToConfirm()
        }
        case None => {
          // The user added an unsolution, but no words matches the puzzle. There is no previous unconfirmed
          // unsolution.
          state.storeUnconfirmedUnsolution(user, unsolution)
          return UnsolutionNeedsConfirmation(state.puzzle.get)
        }
        case Some(text) if text != unsolution && unsolution != "" => {
          // The user added an unsolution, but no words matches the puzzle. There _is_ a previous unconfirmed
          // unsolution. We're overwriting it.
          state.storeUnconfirmedUnsolution(user, unsolution)
          return UnsolutionNeedsConfirmation(state.puzzle.get)
        }
        case Some(text) if unsolution == "" => {
          // The user confirmed an unsolution using the empty add unsolution command. Store the previously saved
          // unconfirmed unsolution.
          state.storeUnsolution(user, text)
          return UnsolutionAdded()
        }
        case default => {
          // In this case the user confirmed an unsolution by repeating it.
        }
      }
    }

    state.storeUnsolution(user, unsolution)
    UnsolutionAdded()
  }

  def listUnsolutions(user: User): Response = {
    val unsolutionsForUser = state.unsolutions().get(user)

    unsolutionsForUser map (Unsolutions(_)) getOrElse NoUnsolutions()
  }

  private def checkSolution(user: User, word: Word, puzzle: Puzzle, isWeekday: Boolean): Response = {
    import WritingSystemHelper._

    if (!(word isNineLetters)) {
      val tooMany: Option[String] = Some(word.letters.norm diff puzzle.letters.norm) filter (s => !s.isEmpty)
      val tooFew: Option[String] = Some(puzzle.letters.norm diff word.letters.norm) filter (s => !s.isEmpty)
      return IncorrectLength(word, tooMany, tooFew)
    }

    if (!(word matches puzzle)) {
      val tooMany = word.letters.norm diff puzzle.letters.norm
      val tooFew = puzzle.letters.norm diff word.letters.norm
      return WordAndPuzzleMismatch(word, puzzle, tooMany, tooFew)
    }
    if (dictionary has word) {
      val userHasSolvedThisBefore = state.hasSolved(user, word)
      state.solved(user, word, isWeekday)
      val noOfSolutions = dictionary.solutions(puzzle).size
      val solutionId: Option[Int] = dictionary.solutionId(state.puzzle(), word) filter (_ => noOfSolutions > 1)
      if (!userHasSolvedThisBefore)
        CompositeResponse(
          Vector(CorrectSolution(word), SolutionNotification(user, state.streak(user), solutionId))
        )
      else
        CorrectSolution(word)
    } else {
      NotInTheDictionary(word)
    }
  }

  private def anyWordMatchesPuzzle(puzzle: Puzzle, unsolution: String): Boolean = {
    val words = unsolution.split(" ") map (Word(_))
    words exists (_ matches puzzle)
  }
}
