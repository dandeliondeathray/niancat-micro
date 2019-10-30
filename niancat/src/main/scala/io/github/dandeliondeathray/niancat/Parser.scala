package io.github.dandeliondeathray.niancat

/** A PuzzleCommand is a command sent by a user to Niancat. */
sealed trait PuzzleCommand {
  def apply(engine: PuzzleEngine): Response
}

case class SetPuzzle(puzzle: Puzzle, isWeekday: Boolean) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = {
    engine.set(puzzle, isWeekday)
  }
}

case class Get() extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = engine.get()
}

case class GetUserStats(user: User) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = engine.getUserStats(user)
}

case class CheckSolution(word: Word, user: User, isWeekday: Boolean) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = {
    engine.check(user, word, isWeekday)
  }
}

case class Ignored() extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = NoResponse()
}

sealed trait InvalidCommandError
case class UnknownCommand(command: String) extends InvalidCommandError
case class WrongArguments(actualNoOfArgs: Int, expectedNoOfArgs: Int) extends InvalidCommandError

case class InvalidCommand(msg: String, errorType: InvalidCommandError) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = InvalidCommandReply(msg, errorType)
}

case class AddUnsolution(unsolution: String, user: User) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = engine.addUnsolution(unsolution, user)
}
case class ListUnsolutions(user: User) extends PuzzleCommand {
  def apply(engine: PuzzleEngine): Response = engine.listUnsolutions(user)
}

sealed trait ChannelVisibility
case class PrivateChannel() extends ChannelVisibility
case class PublicChannel() extends ChannelVisibility

trait Parser {
  def parse(msg: String, user: User, visibility: ChannelVisibility, isWeekday: Boolean): PuzzleCommand
}

class SlackParser extends Parser {
  def parse(msg: String, user: User, visibility: ChannelVisibility, isWeekday: Boolean): PuzzleCommand = {
    if (!msg.startsWith("!")) {
      if (visibility == PrivateChannel()) return CheckSolution(Word(msg), user, isWeekday)

      return Ignored()
    }
    val words = msg.split(" ", 2)

    if (words(0) == "!nian") {
      return Get()
    }

    if (words(0) == "!sättnian") {
      if (words.size != 2) return InvalidCommand(msg, WrongArguments(words.size - 1, 1))
      return SetPuzzle(Puzzle(words(1)), isWeekday)
    }

    if (words(0) == "!olösning") {
      if (words.size != 2) return InvalidCommand(msg, WrongArguments(words.size - 1, 1))
      return AddUnsolution(words(1), user)
    }

    if (words(0) == "!olösningar") {
      return if (visibility == PrivateChannel()) ListUnsolutions(user) else Ignored()
    }

    if (words(0) == "!statistik") {
      return if (visibility == PrivateChannel()) GetUserStats(user) else Ignored()
    }

    if (visibility == PublicChannel()) {
      return Ignored()
    } else {
      return InvalidCommand(msg, UnknownCommand(words(0)))
    }
  }
}
