package io.github.dandeliondeathray.niancat

import org.scalatest._

class TokenSpec extends FlatSpec with Matchers {
  "a Token" should "read from a dot directory in the users home" in {
    val token = "01234567889abcdef"
    // Write the file $HOME/.slack/token_integration_test.token

    // Supply the name token_integration_test and expect the token back
    Token.read("token_integration_test" shouldBe Success(token)
  }
}