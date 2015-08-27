package rssreader.utils

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api.{DB, MongoDriver}

object MongoContext {
  val driver = new MongoDriver
  val connection = driver.connection(List("192.168.1.102"))
  def db: DB = connection("RSSReader")
}
