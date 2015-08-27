package rssreader.utils

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._

/**
 * Provide a default timeout for using whenReady in tests
 */
trait FutureHelper extends ScalaFutures {
  val TIMEOUT = timeout(2 seconds)

  def whenReady[T,U](f: FutureConcept[T])(fun: T => U): U = whenReady(f, TIMEOUT)(fun)

}
