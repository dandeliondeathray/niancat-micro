package io.github.dandeliondeathray.niancat

import org.scalatest._

/**
  * Created by Erik Edin on 2017-05-09.
  */
class NineLetterDictionarySpec extends FlatSpec with Matchers {
  def dictionary = NineLetterDictionary(
    "A",
    "AB",
    "ABC",
    "ABCDEFGHI",
    "BCDEFGHIJ",
    "PIKÉTRÖJA",
    "VANTRIVAS",
    "ABCDEFGHIJKL",
    "BCDEFGHIJKLM",
    "DATORSPEL",
    "SPELDATOR",
    "LEDARPOST",
    "REPSOLDAT"
  )

  "the dictionary" should "contain ABCDEFGHI" in {
    dictionary.has(Word("ABCDEFGHI")) shouldBe true
  }

  it should "not contain missing word PUSSGURKA" in {
    dictionary.has(Word("PUSSGURKA")) shouldBe false
  }

  it should "contain special word PIKÉTRÖJA" in {
    dictionary.has(Word("PIKÉTRÖJA")) shouldBe true
  }

  it should "not contain short word ABC" in {
    dictionary.has(Word("ABC")) shouldBe false
  }

  it should "not contain long word ABCDEFGHIJKL" in {
    dictionary.has(Word("ABCDEFGHIJKL")) shouldBe false
  }

  it should "normalize words in constructor and lookup" in {
    dictionary.has(Word("piketröja")) shouldBe true
  }

  it should "know the all the solutions to a given puzzle" in {
    dictionary.solutions(Puzzle("DATORLESP")).size shouldBe 4
  }

  it should "say that an unknown word has zero solutions" in {
    dictionary.solutions(Puzzle("NOSUCHWRD")).size shouldBe 0
  }

  it should "find a single solution even for a non-normalized puzzle" in {
    dictionary.solutions(Puzzle("piketröja")).size shouldBe 1
  }

  it should "return the same solution id for a given word every time" in {
    val word = Word("DATORSPEL")
    val solutionIds = 1 until 10 map (_ => dictionary.solutionId(Some(Puzzle("DATORSLEP")), word))
    val first = solutionIds.head
    assert(solutionIds forall (_ == first))
  }

  it should "return solution ids 1-4 in some order, if it has four solutions" in {
    val words = Seq(Word("DATORSPEL"), Word("SPELDATOR"), Word("LEDARPOST"), Word("REPSOLDAT"))
    val solutionIds = words map (dictionary.solutionId(Some(Puzzle("DATORSLEP")), _)) flatten

    assert(Set(solutionIds: _*) == Set(1, 2, 3, 4))
  }

  it should "return the solution id even if there is only one solution" in {
    dictionary.solutionId(Some(Puzzle("VANTRIVSA")), Word("VANTRIVAS")) shouldBe Some(1)
  }

  it should "normalize words to find the solution id" in {
    dictionary.solutionId(Some(Puzzle("PIKÉTRÖAJ")), Word("piketröja")) shouldBe Some(1)
  }
}
