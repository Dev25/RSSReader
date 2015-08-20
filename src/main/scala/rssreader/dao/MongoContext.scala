package rssreader.dao

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import collection.JavaConversions._

import reactivemongo.api.{DB, MongoDriver}

object MongoContext {
  val driver = new MongoDriver
  val connection = driver.connection(List("192.168.1.102"))
  def db: DB = connection("RSSReader")
}