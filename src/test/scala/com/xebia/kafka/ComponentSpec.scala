package com.xebia.kafka

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import com.xebia.model.FeedState
import com.xebia.model.Packet

import java.util.UUID

class ComponentSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "the depuplication component" - {
    val state = FeedState(1, None)

    "allow non-dupes" in {
      val packet = Packet[Int](2, "football", "actions", UUID.randomUUID(), 0)
      val expectedState = FeedState(2, None)
      val stream =
        fs2
          .Stream(state -> packet)
          .through(Components.dedupPipe)
          .compile
          .last

      stream.map { res =>
        res should contain(expectedState -> packet)
      }
    }

    "not allow dupes (seqNum equal to state)" in {
      val packet = Packet[Int](1, "football", "actions", UUID.randomUUID(), 0)
      val stream =
        fs2
          .Stream(state -> packet)
          .through(Components.dedupPipe)
          .compile
          .last

      stream.map { res =>
        res shouldBe empty
      }
    }

    "not allow dupes (seqNum less than state)" in {
      val packet = Packet[Int](0, "football", "actions", UUID.randomUUID(), 0)
      val stream =
        fs2
          .Stream(state -> packet)
          .through(Components.dedupPipe)
          .compile
          .last

      stream.map { res =>
        res shouldBe empty
      }
    }
  }

  "the transformation component" - {
    "can transform a Packet[Int] to Packet[String]" in {
      val state = com.xebia.model.FeedState.empty
      val feedId = UUID.randomUUID()
      val packetIn = Packet[Int](1, "football", "actions", feedId, 1)
      val packetOut = Packet[String](1, "football", "actions", feedId, "1")

      val stream =
        fs2
          .Stream(state -> packetIn)
          .through(Components.transformPipe { (s, p) =>
            IO.pure(s -> Packet(p.seqNum, p.sport, p.feedType, p.feedId, p.payload.toString()))
          })
          .compile
          .last

      stream.map { res =>
        res should contain(state -> packetOut)
      }
    }
  }

}
