package io.github.dandeliondeathray.niancat

import org.scalactic.NormMethods._
import PuzzleNormalizer._
import WordNormalizer._

case class SolutionResult(wordsAndSolvers: Map[Word, Seq[User]] = Map())

trait State {
  def puzzle(): Option[Puzzle]
  def unsolutions(): Map[User, Seq[String]]
  def unconfirmedUnsolutions(): Map[User, String]

  def result(allSolutions: Seq[Word]): Option[SolutionResult]
  def reset(p: Puzzle, isWeekday: Boolean): Unit
  def solved(user: User, word: Word, isWeekday: Boolean): Unit
  def hasSolved(user: User, word: Word): Boolean
  def streak(user: User): Int
  def streaks(): Map[User, Int]

  def storeUnsolution(user: User, text: String)
  def storeUnconfirmedUnsolution(user: User, text: String)
}

trait StateRepr {
  var puzzle: Option[Puzzle]
  var unsolutions: Map[User, Seq[String]]
  var unconfirmedUnsolutions: Map[User, String]
  var solutions: Seq[(Word, User)]
  var streaks: Map[User, Int]
}

class NiancatState(var repr: StateRepr) extends State {
  override def puzzle(): Option[Puzzle] = repr.puzzle

  override def unsolutions() = repr.unsolutions

  override def unconfirmedUnsolutions() = repr.unconfirmedUnsolutions

  override def result(allSolutions: Seq[Word]): Option[SolutionResult] = {
    puzzle match {
      case None => None
      case Some(p) => {
        val allSolutionsMap: Map[Word, Seq[User]] = (allSolutions map (_ -> Seq[User]())).toMap
        val foundSolutions: Map[Word, Seq[User]] = (repr.solutions groupBy (_._1) mapValues (_.map(_._2).toSeq))
        Some(SolutionResult(allSolutionsMap ++ foundSolutions))
      }
    }
  }

  override def streaks() = {
    repr.streaks.toMap
  }

  override def reset(p: Puzzle, isWeekday: Boolean): Unit = {
    repr.puzzle = Some(p.norm)
    if (isWeekday) {
      repr.streaks = repr.streaks filter { case (user, _) => repr.solutions.map(_._2) contains user }
    }
    repr.solutions = Seq()
    repr.unsolutions = Map()
    repr.unconfirmedUnsolutions = Map()
  }
  override def solved(user: User, word: Word, isWeekday: Boolean): Unit = {
    val userHasFoundThisSolutionBefore = repr.solutions contains (word.norm, user)
    if (!userHasFoundThisSolutionBefore) {
      repr.solutions :+= (word.norm, user)
      if (isWeekday) {
        repr.streaks += user -> ((repr.streaks getOrElse (user, 0)) + 1)
      }
    }
  }

  override def hasSolved(user: User, word: Word): Boolean = {
    repr.solutions contains (word.norm, user)
  }
  override def streak(user: User): Int = repr.streaks getOrElse (user, 0)

  override def storeUnsolution(user: User, text: String): Unit = {
    val unsolutionsForUser: Seq[String] = repr.unsolutions.getOrElse(user, Seq[String]())
    repr.unsolutions += (user -> (unsolutionsForUser :+ text))
    repr.unconfirmedUnsolutions -= user
  }

  override def storeUnconfirmedUnsolution(user: User, text: String): Unit = {
    repr.unconfirmedUnsolutions += (user -> text)
  }
}
