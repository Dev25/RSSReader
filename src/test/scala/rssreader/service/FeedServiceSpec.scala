package rssreader.service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import reactivemongo.bson.BSONObjectID
import rssreader.core.Feed
import rssreader.dao.FeedDao
import rssreader.utils.MongoContext
import rssreader.utils.tests.MongoTestSpec

class FeedServiceSpec extends MongoTestSpec with MockitoSugar {

  val mongoCollection = "RSSReader-FeedServiceSpec"

  trait NewFeed {
    // Default feed for test usage, override if another feed is required
    val url = fileExampleFeed
    val feed = Feed.parse(url).get

    // Run the callback function once the feed is saved
    def whenInserted[U](callback: (Feed) => U) = {
      val future = service.saveFeed(url)
      whenReady(future)(_.map(callback))
    }
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
      service.exists(f._id).futureValue shouldBe true
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
    val files = validFiles
    val futures: List[Future[Option[Feed]]] = files.map(service.saveFeed)
    whenReady(Future.sequence(futures)){ feeds =>
      service.count().futureValue shouldBe files.size // Check size, if all feeds saved
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
    val future = service.saveFeed(fileInvalidFeed)
    whenReady(future){ result =>
      result shouldEqual None
      service.exists(fileInvalidFeed.toString).futureValue shouldBe false // feed was not saved
    }
  }

  behavior of "Updating existing feeds"
  it should "update feed metadata + add any new items"  in new NewFeed {
    whenInserted { insertedFeed  => // Insert original exampleFeed

      // Create our spied DAO
      val mDao = spy(new FeedDao(MongoContext.connection(mongoCollection)))
      val fakeService = new FeedService { override val dao = mDao }

      // Stub our DAO findById method so it returns our updated feed object
      val updatedFeed = Feed.parse(fileExampleFeedUpdated, insertedFeed._id).get
      when(mDao.findById(insertedFeed._id)).thenReturn(Future.successful(Some(updatedFeed)))

      // Run our update function
      val updatedFuture = fakeService.updateFeed(insertedFeed._id)
      whenReady(updatedFuture){ _ =>
        // DB feed item should now be updated to reflect changes from fileExampleFeedUpdated
        val updatedDbFeed = service.findById(insertedFeed._id).futureValue.get

        // DB feed should no longer match either original or updated feed
        updatedDbFeed should not equal insertedFeed
        updatedDbFeed should not equal updatedFeed


        updatedDbFeed.items should have size 2 // 1 from each version of the feed original/updated
        updatedDbFeed shouldEqual updatedFeed.copy(items = updatedDbFeed.items) // Equal latest metadata + all items
      }
    }
  }

  it should "update all existing feeds" in {
    // Setup our service with a mocked dao + spied instance to use for testing
    class FakedService extends FeedService { override val dao = mock[FeedDao] } // Due to Mockito restrictions
    val mService = spy(new FakedService)

    val ids = Seq.fill(5)(BSONObjectID.generate).toList // Create 5 random feed ids

    // Setup spying
    when(mService.dao.findAllIds()).thenReturn(Future.successful(ids)) // Return our specified ids
    ids.foreach(id => doReturn(Future.successful(true)).when(mService).updateFeed(id)) // Return true when updateFeed(id) is called

    // Perform action
    mService.updateAllFeeds()

    // Verify the correct actions take place (find all ids + perform update on each id)
    verify(mService.dao).findAllIds()
    ids.foreach{ id => verify(mService).updateFeed(id)}
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
