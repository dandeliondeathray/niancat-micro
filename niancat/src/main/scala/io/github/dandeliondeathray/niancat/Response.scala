package io.github.dandeliondeathray.niancat

object DisplayHelper {
  implicit class PuzzleDisplay(p: Puzzle) {
    def show: String = p.letters grouped (3) mkString (" ")
  }

  implicit class SolutionResultDisplay(s: SolutionResult) {
    def display: Seq[String] = {
      val wordsAndSolvers = s.wordsAndSolvers.map(kv => showWordAndSolution(kv._1, kv._2))
      Seq("*Gårdagens lösningar:*") ++ wordsAndSolvers.toSeq
    }

    private def showWordAndSolution(w: Word, solvers: Seq[User]): String = {
      s"*${w.letters}*: " ++ solvers.map(_.name).mkString(", ")
    }
  }

  implicit class StreaksDisplay(s: Map[User, Int]) {
    def display: Seq[String] = {
      val streaks = s.toList.filter(_._2 > 1).groupBy(_._2).toList.sortBy(-_._1).map(showStreak)
      if (streaks.nonEmpty) Seq("*Obrutna serier:*") ++ streaks.toSeq else Seq()
    }

    private def showStreak(streak: (Int, Seq[(User, Int)])): String =
      s"${streak._1}: ${streak._2.map(_._1.name).mkString(", ")}"
  }
}

import DisplayHelper._

sealed trait Response {
  def toResponse: String
}
sealed trait Reply extends Response
sealed trait Notification extends Response

case class CompositeResponse(responses: Vector[Response]) extends Response {
  override def toResponse: String = ""
}
case class NoResponse() extends Response {
  override def toResponse: String = ""
}

case class NoPuzzleSet() extends Reply {
  override def toResponse: String = "Nian är inte satt."
}
case class GetReply(puzzle: Puzzle) extends Reply {
  override def toResponse: String = puzzle show
}
case class NotInTheDictionary(word: Word) extends Reply {
  override def toResponse: String = s"Ordet ${word.letters} finns inte med i SAOL."
}
case class CorrectSolution(word: Word) extends Reply {
  override def toResponse: String = s"Ordet ${word.letters} är korrekt!"
}
case class WordAndPuzzleMismatch(word: Word, puzzle: Puzzle, tooMany: String, tooFew: String) extends Reply {
  override def toResponse: String =
    s"Ordet ${word.letters} matchar inte nian ${puzzle show}. " +
      s"För många $tooMany. För få $tooFew"

}
case class IncorrectLength(word: Word, tooMany: Option[String], tooFew: Option[String]) extends Reply {
  override def toResponse: String = {
    val parts = List(
      Some(s"${word.letters} är inte nio tecken långt."),
      tooMany map (t => s" För många $t."),
      tooFew map (t => s" För få $t.")
    )
    parts.flatten.mkString
  }

}
case class InvalidPuzzle(puzzle: Puzzle) extends Reply {
  override def toResponse: String = s"Pusslet ${puzzle show} är inte giltigt!"
}
case class InvalidCommandReply(msg: String, invalidCommandError: InvalidCommandError) extends Reply {
  override def toResponse = {
    invalidCommandError match {
      case UnknownCommand(cmd: String) => s"Okänt kommando $cmd"
      case WrongArguments(actual: Int, expected: Int) =>
        s"Fel antal argument. Fick $actual men förväntade $expected"
    }
  }
}
case class SamePuzzle(puzzle: Puzzle) extends Reply {
  override def toResponse = s"Pusslet ${puzzle show} är redan satt!"
}

case class Unsolutions(texts: Seq[String]) extends Reply {
  override def toResponse = texts mkString ("\n")
}

case class NoUnsolutions() extends Reply {
  override def toResponse = "Inga olösningar sparade."
}

case class UnsolutionAdded() extends Reply {
  override def toResponse = "Sparat."
}

case class UnsolutionNeedsConfirmation(puzzle: Puzzle) extends Reply {
  override def toResponse = s"Inget ord i olösningen matchar pusslet ${puzzle show}. Spara igen för att bekräfta."
}

case class NoUnsolutionToConfirm() extends Reply {
  override def toResponse = s"Det finns ingen olösning att bekräfta."
}

case class AllUnsolutions(unsolutionsForEachUser: Map[User, Seq[String]]) extends Notification {
  private def usersUnsolutionToString(entry: (User, Seq[String])): String = {
    val name = entry._1.name
    val unsolutions = "\n • " ++ entry._2.mkString("\n • ")
    s"$name: $unsolutions"
  }

  override def toResponse = {
    val unsolutionsAsString = unsolutionsForEachUser.toSeq map (usersUnsolutionToString(_)) mkString ("\n")
    s"*Olösningar*:\n$unsolutionsAsString"
  }
}

case class NewPuzzle(puzzle: Puzzle) extends Notification {
  override def toResponse: String = {
    s"Dagens nian är ${puzzle show}"
  }
}
case class YesterdaysPuzzle(result: SolutionResult) extends Notification {
  override def toResponse: String = {
    result.display mkString ("\n")
  }
}
case class Streaks(streaks: Map[User, Int]) extends Notification {
  override def toResponse: String = {
    streaks.display mkString ("\n")
  }
}
case class MultipleSolutions(n: Int) extends Notification {
  override def toResponse = s":bangbang: Dagens nian har $n lösningar. :bangbang:"
}

case class SolutionNotification(user: User, streak: Int, solutionId: Option[Int] = None) extends Notification {
  override def toResponse = {
    val parts = List(
      Some(s"${user.name} löste nian"),
      solutionId map (n => s", ord $n"),
      Some("!"),
      Some(" :niancat:" * streak)
    )
    parts.flatten.mkString
  }
}
