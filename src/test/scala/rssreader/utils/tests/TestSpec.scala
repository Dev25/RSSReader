package rssreader.utils.tests

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import rssreader.dao.FeedDao
import rssreader.service.FeedService
import rssreader.utils.MongoContext


/** TestSpec for all actor related tests */
abstract class ActorTestKitSpec extends TestKit(ActorSystem("testSystem")) with FlatSpecLike with BeforeAndAfterAll
                                   with ImplicitSender with FutureHelper {

  override def afterAll() = TestKit.shutdownActorSystem(system)
}


/** Base TestSpec with common trait mixins */
abstract class TestSpec extends FlatSpec with Matchers with FutureHelper with TestFiles


/** A TestSpec for tests that use the mongo backend, the collection is before each test and after all tests. */
abstract class MongoTestSpec extends TestSpec with BeforeAndAfterEach with BeforeAndAfterAll with MongoService {

  val DEFAULT_MONGO_TIMEOUT = 2 seconds span

  override def beforeEach() = Await.ready(dao.drop(), DEFAULT_MONGO_TIMEOUT)

  override def afterAll() = Await.ready(dao.drop(), DEFAULT_MONGO_TIMEOUT)
}


/** Load a Feed Service/DAO using a specified mongo collection */
trait MongoService {
  /** Collection name to be used for tests, use unique names to prevent errors when running tests in parallel */
  val mongoCollection: String
  lazy val dao = new FeedDao(MongoContext.connection(mongoCollection))
  lazy val service = new FeedService { override val dao = MongoService.this.dao }
}


/** Collection of test file resources */
trait TestFiles {
  final val fileExampleFeed = getClass.getResource("/exampleFeed.xml")
  final val fileExampleFeedUpdated = getClass.getResource("/exampleFeedUpdated.xml")
  final val file10ItemFeed = getClass.getResource("/10ItemFeed.xml")
  final val fileMinimalFeed = getClass.getResource("/minimalFeed.xml")
  final val fileMultipleItemFeed = getClass.getResource("/multipleItemFeed.xml")
  final val fileInvalidFeed = getClass.getResource("/malformedFeed.xml")

  final val validFiles = List(fileExampleFeed, file10ItemFeed, fileMinimalFeed, fileMultipleItemFeed)
}
