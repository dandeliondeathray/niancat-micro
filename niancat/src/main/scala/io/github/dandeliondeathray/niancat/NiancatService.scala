package io.github.dandeliondeathray.niancat

import io.circe._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import io.circe.generic.auto._

import java.time.{LocalDate, DayOfWeek}

case class SetPuzzleBody(puzzle: String)
case class CheckSolutionBody(user: String, solution: String)
case class AddUnsolutionBody(unsolution: String)

object NiancatService {
  def service(dictionary: Dictionary) = {
    val state = new NiancatState()
    val engine = new PuzzleEngine(state, dictionary)
    val responder = new NiancatApiResponder()

    HttpService {

      case GET -> Root / "v1" / "puzzle" => {
        val command = Get()
        val response = command(engine)
        val messageResponses = responder.messageResponses(response)
        Ok(Json.fromValues(messageResponses map (_.toJSON)))
      }
      case req @ POST -> Root / "v1" / "puzzle" => {
        val yesterdayWasWeeekday = List(
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY,
          DayOfWeek.SATURDAY
        ) contains LocalDate.now().getDayOfWeek()

        req.as(jsonOf[SetPuzzleBody]) flatMap { setPuzzleBody =>
          val command = SetPuzzle(Puzzle(setPuzzleBody.puzzle), yesterdayWasWeeekday)
          val response = command(engine)
          val messageResponses = responder.messageResponses(response)
          Ok(Json.fromValues(messageResponses map (_.toJSON)))
        }
      }
      case req @ POST -> Root / "v1" / "solution" => {
        val isWeekday = List(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        ) contains LocalDate.now().getDayOfWeek()

        req.as(jsonOf[CheckSolutionBody]) flatMap { checkSolutionBody =>
          val command = CheckSolution(Word(checkSolutionBody.solution), User(checkSolutionBody.user), isWeekday)
          val response = command(engine)
          val messageResponses = responder.messageResponses(response)
          Ok(Json.fromValues(messageResponses map (_.toJSON)))
        }
      }
      case GET -> Root / "v1" / "unsolution" / user => {
        val command = ListUnsolutions(User(user))
        val response = command(engine)
        val messageResponses = responder.messageResponses(response)
        Ok(Json.fromValues(messageResponses map (_.toJSON)))
      }
      case req @ POST -> Root / "v1" / "unsolution" / user => {
        req.as(jsonOf[AddUnsolutionBody]) flatMap { addUnsolutionBody =>
          val command = AddUnsolution(addUnsolutionBody.unsolution, User(user))
          val response = command(engine)
          val messageResponses = responder.messageResponses(response)
          Ok(Json.fromValues(messageResponses map (_.toJSON)))
        }
      }
      case GET -> Root / "v1" / "user" / user / "stats" => {
        val command = GetUserStats(User(user))
        val response = command(engine)
        val messageResponses = responder.messageResponses(response)
        Ok(Json.fromValues(messageResponses map (_.toJSON)))
      }
      case GET -> Root / "health" => {
        Ok()
      }
      case GET -> Root / "readiness" => {
        Ok()
      }
    }
  }
}
