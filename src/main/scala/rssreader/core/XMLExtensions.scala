package rssreader.core

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import scala.xml.NodeSeq

import org.scalactic.{Bad, ErrorMessage, Every, Good, One, Or}

object XMLExtensions {

  implicit class ExtendedNodeSeq(nodeSeq: NodeSeq) {

    /** Parse a node value as an Option */
    def textOption: Option[String] =
      if (nodeSeq.text.isEmpty) None else Some(nodeSeq.text)

    /**
     * Parse a non-empty node value or return an error
     */
    def requiredText(error: ErrorMessage): Or[String, Every[ErrorMessage]] =
      if (nodeSeq.text.isEmpty) Bad(One(error)) else Good(nodeSeq.text)

    /**
     * Parse a node as a Int and return as an Option.
     */
    def asIntOpt: Or[Option[Int], Every[ErrorMessage]] = {
      if (nodeSeq.text.isEmpty) Good(None)
      else
        try Good(Some(nodeSeq.text.toInt))
        catch {
          case _: NumberFormatException => Bad(One("Cannot parse string to Int"))
        }
    }

    /**
     * Parse a node as a LocalDateTime and return as an Option.
     * Returning an error if the date cannot be parsed with RFC1123 or RFC822.
     */
    def dateTimeOption: Or[Option[LocalDateTime], Every[ErrorMessage]] = {
      if (nodeSeq.text.isEmpty) Good(None)
      else
        try Good(Some(LocalDateTime.parse(nodeSeq.text, DateTimeFormatter.RFC_1123_DATE_TIME)))
        catch {
          case e: DateTimeParseException => Bad(One(s"Cannot parse `${e.getParsedString}` using RFC 1123/822"))
        }
    }
  }

}
