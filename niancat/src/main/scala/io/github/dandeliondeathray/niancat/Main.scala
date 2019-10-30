package io.github.dandeliondeathray.niancat

import scala.io.Source
import scala.util.{Failure, Success, Try}
import java.util.concurrent.{ExecutorService, Executors}
import scala.util.Properties.envOrNone
import scalaz.concurrent.Task
import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze.BlazeBuilder

object NiancatSlackApp extends ServerApp {
  val port: Int = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  val ip: String = "0.0.0.0"
  val pool: ExecutorService = Executors.newSingleThreadExecutor()

  override def server(args: List[String]): Task[Server] = {
    if (args.size == 2 && args(0) == "filter") {
      filter(args(1))
      System.exit(0)
    }

    if (args.size != 0) {
      println("""
         Usage: niancat
                    or
                niancat filter <dictionary>
      """.stripMargin)
      System.exit(1)
    }

    val dictionaryFile = sys.env.get("DICTIONARY_FILE")
    if (dictionaryFile.isEmpty) {
      println("There must be an environment variable DICTIONARY_FILE set!")
      System.exit(1)
    }

    val tryDictionary: Try[Dictionary] = Try {
      val lines = Source.fromFile(dictionaryFile.get).getLines().toSeq
      new NineLetterDictionary(lines)
    }

    val dictionary: Dictionary = tryDictionary match {
      case Failure(e) => {
        println(s"Could not read dictionary in file $dictionaryFile: Got exception $e")
        System.exit(1)
        NineLetterDictionary()
      }
      case Success(d: Dictionary) => d
    }

    BlazeBuilder
      .bindHttp(port, ip)
      .mountService(NiancatService.service(dictionary))
      .withServiceExecutor(pool)
      .start
  }

  def filter(dictionaryFile: String): Unit = {
    val maybeDictionary: Try[NineLetterDictionary] = Try {
      val lines = Source.fromFile(dictionaryFile).getLines().toSeq
      new NineLetterDictionary(lines)
    }

    val filtered: Dictionary = maybeDictionary match {
      case Failure(e) => {
        println(s"Could not read dictionary in file $dictionaryFile: Got exception $e")
        System.exit(1)
        NineLetterDictionary()
      }
      case Success(d: NineLetterDictionary) => d
    }

    (filtered.toSeq map ((w: Word) => w.letters) sorted) foreach (println(_))
  }
}
