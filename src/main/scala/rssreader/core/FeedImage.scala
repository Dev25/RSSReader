package rssreader.core

import scala.xml.NodeSeq

import org.scalactic.Accumulation._
import org.scalactic.{ErrorMessage, Every, Or}
import play.api.libs.json.Json
import rssreader.utils.XMLExtensions
import rssreader.utils.XMLExtensions._

case class FeedImage(url: String,
                     title: String,
                     link: String,
                     width: Option[Int],
                     height: Option[Int])

object FeedImage {
  implicit val fmt = Json.format[FeedImage]

  def parse(node: NodeSeq): Or[FeedImage, Every[ErrorMessage]] = {
    withGood(
      (node \ "url").requiredText("Missing feed image url"),
      (node \ "title").requiredText("Missing feed image title "),
      (node \ "link").requiredText("Missing feed image link"),
      (node \ "width").asIntOpt,
      (node \ "height").asIntOpt
    )(FeedImage.apply)
  }
}