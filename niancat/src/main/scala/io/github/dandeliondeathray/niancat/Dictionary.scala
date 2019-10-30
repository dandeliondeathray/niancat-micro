package io.github.dandeliondeathray.niancat

trait Dictionary {
  def has(word: Word): Boolean
  def toSeq: Seq[Word]
  def solutions(puzzle: Puzzle): Seq[Word]
  def solutionId(puzzle: Option[Puzzle], word: Word): Option[Int]
}
