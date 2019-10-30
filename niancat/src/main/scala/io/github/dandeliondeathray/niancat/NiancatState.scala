package io.github.dandeliondeathray.niancat

import org.scalactic.NormMethods._
import PuzzleNormalizer._
import WordNormalizer._

case class SolutionResult(wordsAndSolvers: Map[Word, Seq[User]] = Map(), streaks: Map[User, Int] = Map())

trait State {
  def puzzle(): Option[Puzzle]
  def unsolutions(): Map[User, Seq[String]]
  def unconfirmedUnsolutions(): Map[User, String]

  def result(allSolutions: Seq[Word]): Option[SolutionResult]
  def reset(p: Puzzle, isWeekday: Boolean): Unit
  def solved(user: User, word: Word, isWeekday: Boolean): Unit
  def hasSolved(user: User, word: Word): Boolean
  def streak(user: User): Int
  def userState(user: User): UserState

  def countAttempt(user: User, validAttempt: Boolean)
  def storeUnsolution(user: User, text: String)
  def storeUnconfirmedUnsolution(user: User, text: String)
}

case class UserState(
  validAttempts: Int = 0,
  invalidAttempts: Int = 0,
) {
  def totalAttempts: Int = invalidAttempts + validAttempts
}

class NiancatState extends State {
  private var _puzzle: Option[Puzzle] = None
  private var _unsolutions: Map[User, Seq[String]] = Map()
  private var _unconfirmedUnsolutions: Map[User, String] = Map()
  private var solutions: Seq[(Word, User)] = Seq()
  private var streaks: Map[User, Int] = Map()
  private var userStates: Map[User, UserState] = Map()

  override def puzzle(): Option[Puzzle] = _puzzle

  override def unsolutions() = _unsolutions

  override def unconfirmedUnsolutions() = _unconfirmedUnsolutions

  override def result(allSolutions: Seq[Word]): Option[SolutionResult] = {
    puzzle match {
      case None => None
      case Some(p) => {
        val allSolutionsMap: Map[Word, Seq[User]] = (allSolutions map (_ -> Seq[User]())).toMap
        val foundSolutions: Map[Word, Seq[User]] = (solutions groupBy (_._1) mapValues (_.map(_._2).toSeq))
        Some(SolutionResult(allSolutionsMap ++ foundSolutions, streaks.toMap))
      }
    }
  }

  override def reset(p: Puzzle, isWeekday: Boolean): Unit = {
    _puzzle = Some(p.norm)
    if (isWeekday) {
      streaks = streaks filter { case (user, _) => solutions.map(_._2) contains user }
    }
    solutions = Seq()
    _unsolutions = Map()
    _unconfirmedUnsolutions = Map()
    userStates = Map()
  }
  override def solved(user: User, word: Word, isWeekday: Boolean): Unit = {
    val userHasFoundThisSolutionBefore = solutions contains ((word.norm, user))
    if (!userHasFoundThisSolutionBefore) {
      solutions :+= (word.norm, user)
      if (isWeekday) {
        streaks += user -> ((streaks getOrElse (user, 0)) + 1)
      }
    }
  }

  override def hasSolved(user: User, word: Word): Boolean = {
    solutions contains ((word.norm, user))
  }
  override def streak(user: User): Int = streaks getOrElse (user, 0)
  override def userState(user: User): UserState = userStates getOrElse (user, UserState())

  override def countAttempt(user: User, validAttempt: Boolean): Unit = {
    val currentState = userState(user)
    val updatedState =
      if (validAttempt)
        currentState.copy(validAttempts = currentState.validAttempts + 1)
      else
        currentState.copy(invalidAttempts = currentState.invalidAttempts + 1)
    userStates = userStates.updated(user, updatedState)
  }

  override def storeUnsolution(user: User, text: String): Unit = {
    val unsolutionsForUser: Seq[String] = _unsolutions.getOrElse(user, Seq[String]())
    _unsolutions += (user -> (unsolutionsForUser :+ text))
    _unconfirmedUnsolutions -= user
  }

  override def storeUnconfirmedUnsolution(user: User, text: String): Unit = {
    _unconfirmedUnsolutions += (user -> text)
  }
}
