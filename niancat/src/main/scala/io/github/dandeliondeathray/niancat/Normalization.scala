package io.github.dandeliondeathray.niancat
import java.text.Normalizer
import org.scalactic._
import NormMethods._

object StringNormalizer {
  implicit val stringNormalizer = new Normalization[String] {
    def normalized(s: String): String = {
      val decomposed = Normalizer.normalize(s, Normalizer.Form.NFKD).replaceAll("[- _\u0301\u0341]", "").toUpperCase
      Normalizer.normalize(decomposed, Normalizer.Form.NFKC)
    }
  }
}
import StringNormalizer._

object PuzzleNormalizer {
  implicit val puzzleNormalizer = new Normalization[Puzzle] {
    def normalized(puzzle: Puzzle): Puzzle = Puzzle(puzzle.letters.norm)
  }
}

object WordNormalizer {
  implicit val wordNormalizer = new Normalization[Word] {
    def normalized(word: Word): Word = {
      Word(stringNormalizer.normalized(word.letters))
    }
  }
}
