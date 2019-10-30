package io.github.dandeliondeathray.niancat

import WritingSystemHelper._
import org.scalactic.NormMethods._
import WordNormalizer._
import PuzzleNormalizer._

/**
  * Created by Erik Edin on 2017-05-09.
  */
class NineLetterDictionary(strings: Seq[String]) extends Dictionary {
  val dictionary = strings map (Word(_).norm) filter (_.isNineLetters) toSet
  val answers: Map[String, Seq[String]] = dictionary.toSeq map (_.norm.letters) groupBy sortByCodePoints

  override def has(word: Word): Boolean = dictionary.contains(word.norm)
  override def toSeq: Seq[Word] = dictionary.toSeq

  override def solutions(puzzle: Puzzle): Seq[Word] =
    answers getOrElse (sortByCodePoints(puzzle.norm.letters), Seq()) map Word

  override def solutionId(puzzle: Option[Puzzle], word: Word): Option[Int] = {
    puzzle
      .map(solutions)
      .map(_ indexOf (word.norm))
      .filter(_ != -1)
      .map(_ + 1)
  }

  private def sortByCodePoints(s: String): String = {
    val codePoints = s.codePoints() sorted () toArray
    val sb = new java.lang.StringBuilder()
    codePoints foreach (sb.appendCodePoint(_))
    sb.toString()
  }
}

object NineLetterDictionary {
  def apply(strings: String*): NineLetterDictionary = {
    new NineLetterDictionary(strings)
  }
}
