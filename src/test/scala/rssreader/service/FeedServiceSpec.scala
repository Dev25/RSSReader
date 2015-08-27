package rssreader.service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import org.scalactic._
import org.mockito.Mockito._
import org.scalatest._
import org.scalactic.Accumulation._
import org.scalatest.mock.MockitoSugar

import rssreader.core.Feed
import rssreader.dao.FeedDao
import rssreader.utils.{MongoContext, FutureHelper}

class FeedServiceSpec extends FlatSpec
                         with org.scalatest.Matchers
                         with BeforeAndAfter
                         with MockitoSugar
                         with FutureHelper {

  val service = new FeedService {
    override val dao = new FeedDao(MongoContext.connection("RSSReader-test"))
  }

  trait NewFeed {
    // Default feed for test usage, override if another feed is required
    val url = getClass.getResource("/exampleFeed.xml")
    val feed = Feed.parse(url).get

    // Run the callback function once the feed is saved
    def whenInserted[U](callback: (Feed) => U) = {
      val future = service.saveFeed(url)
      whenReady(future)(_.map(callback))
    }
  }

  /** DB is wiped before each test */
  before {
    Await.ready(service.dao.drop(), 5 seconds span)
  }


  behavior of "Counting number of feeds saved"
  it should "return 0 when no feeds saved" in {
    service.count().futureValue shouldBe 0
  }

  it should "return 1 when new feed inserted" in new NewFeed {
    whenInserted { _ =>
      service.count().futureValue shouldBe 1
    }
  }

  behavior of "Checking if a feed exists"
  it should "return true for existing feeds by id" in new NewFeed {
    whenInserted { f =>
      service.exists(feed._id).futureValue shouldBe true
    }
  }

  it should "return false if feed does not exist by id" in new NewFeed {
    service.exists(feed._id).futureValue shouldBe false
  }

  it should "return true for existing feeds by url" in new NewFeed {
    whenInserted { f =>
      service.exists(feed.rssUrl).futureValue shouldBe true
    }
  }

  it should "return false if feed does not exist by url" in new NewFeed {
    service.exists(feed.rssUrl).futureValue shouldBe false
  }

  behavior of "Querying Feeds"
  it should "find feed by id" in new NewFeed {
    whenInserted { f =>
      service.findById(f._id).futureValue shouldBe Some(f)
    }
  }

  it should "find feed by url" in new NewFeed {
    whenInserted { f =>
      service.findByUrl(f.rssUrl).futureValue shouldBe Some(f)
    }
  }

  it should "find feed by title" in new NewFeed {
    whenInserted { f =>
      service.findByTitle(f.title).futureValue shouldBe Some(f)
    }
  }

  it should "return all feeds" in  {
    val testFeedNames = List("/exampleFeed.xml", "/10ItemFeed.xml", "/minimalFeed.xml", "/multipleItemFeed.xml")
    val urls = testFeedNames.map(getClass.getResource)
    val futures: List[Future[Option[Feed]]] = urls.map(service.saveFeed)
    whenReady(Future.sequence(futures)){ feeds =>
      service.count().futureValue shouldBe testFeedNames.size // Check size, if all feeds saved
      service.findAll().futureValue should contain theSameElementsAs feeds.flatten // check contents
    }
  }

  behavior of "Saving new feeds"
  it should "save new valid feed from url" in new NewFeed {
    whenInserted { f =>
      service.exists(f.rssUrl).futureValue shouldBe true
    }
  }

  it should "fail when attempting to save malformed feed" in {
    val url = getClass.getResource("/malformedFeed.xml")
    val future = service.saveFeed(url)
    whenReady(future){ result =>
      result shouldEqual None
      service.exists(url.toString).futureValue shouldBe false // feed was not saved
    }
  }

  behavior of "Updating existing feeds"
  it should "update feed metadata + add any new items"  in new NewFeed {
    whenInserted { insertedFeed  => // Insert original feed using normal service
      val mDao = spy(new FeedDao(MongoContext.connection("RSSReader-test"))) // Create a fake service using a dao that returns the original feed with the rssUrl set to our updatedFeed path
      val fakeService = new FeedService { override val dao = mDao }

      val updatedFeed = Feed.parse(getClass.getResource("/exampleFeedUpdated.xml"), insertedFeed._id).get
      when(mDao.findById(insertedFeed._id)).thenReturn(Future.successful(Some(updatedFeed)))

      val updatedFuture = fakeService.updateFeed(insertedFeed._id)
      whenReady(updatedFuture){ _ =>
        val updatedDbFeed = service.findById(insertedFeed._id).futureValue.get
        updatedDbFeed should not equal insertedFeed
        updatedDbFeed should not equal updatedFeed
        updatedDbFeed.items should have size 2 // 1 from each version of the feed when saved/updated
        updatedDbFeed shouldEqual updatedFeed.copy(items = updatedDbFeed.items) // Equal latest metadata + all items
      }
    }
  }

  behavior of "Deleting feed"
  it should "delete existing feed" in new NewFeed {
    whenInserted { f =>
      whenReady(service.deleteFeed(f._id)){ _ =>
        service.exists(f._id).futureValue shouldBe false
      }
    }
  }
}
