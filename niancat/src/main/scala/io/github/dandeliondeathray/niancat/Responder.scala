package io.github.dandeliondeathray.niancat

import io.circe.Json

case class Channel(id: String) {
  def visibility: ChannelVisibility = {
    if (id startsWith ("D")) {
      PrivateChannel()
    } else {
      PublicChannel()
    }
  }
}
case class MessageResponse(responseType: String, msg: String) {
  def toJSON = Json.obj("response_type" -> Json.fromString(responseType), "message" -> Json.fromString(msg))
}

trait Responder {
  def messageResponses(response: Response): Seq[MessageResponse]
}

class NiancatApiResponder extends Responder {
  def messageResponses(response: Response): Seq[MessageResponse] = {
    response match {
      case n: Notification =>
        Seq(MessageResponse("notification", n toResponse))
      case r: Reply =>
        Seq(MessageResponse("reply", r toResponse))
      case NoResponse() => Seq()
      case CompositeResponse(v: Vector[Response]) =>
        v flatMap (messageResponses(_))
    }
  }
}
