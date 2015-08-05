package rssreader.core

import scala.xml.NodeSeq

import org.scalactic.Accumulation._
import org.scalactic.{ErrorMessage, Every, Or}
import rssreader.core.XMLExtensions._

case class FeedImage(url: String,
                     title: String,
                     link: String,
                     width: Option[Int],
                     height: Option[Int])

object FeedImage {
  def apply(node: NodeSeq) = parse(node)

  /**
   * Parse and validate a FeedImage for a Feed.
   */
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