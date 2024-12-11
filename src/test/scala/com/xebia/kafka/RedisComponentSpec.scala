package com.xebia.kafka

import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO

import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import com.dimafeng.testcontainers.RedisContainer
import dev.profunktor.redis4cats._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats._
import org.scalatest.compatible.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import java.util.UUID

class RedisComponentSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with TestContainerForEach {

  override val containerDef =
    RedisContainer.Def(
      DockerImageName.parse("redis:7")
    )

  def withRedis(
    test: Resource[IO, RedisCommands[IO, String, String]] => IO[Assertion]
  ): IO[Assertion] =
    withContainers { container =>
      implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
      val redisCodec: RedisCodec[String, String] =
        RedisCodec.Utf8
      val redisComms: Resource[IO, RedisCommands[IO, String, String]] =
        RedisClient[IO]
          .from(container.redisUri)
          .flatMap(Redis[IO].fromClient(_, redisCodec))
      test(redisComms)
    }

  "the retrieve state pipe" - {
    "retrieve the state from redis" in withRedis { implicit redis =>
      val expectedState = FeedState(2, None)
      val feedId = UUID.randomUUID()
      val packet = Packet[Int](2, "football", "actions", feedId, 0)
      val hashKey = s"${packet.sport}:${packet.feedType}:${packet.feedId}:state"

      val compiledStream =
        fs2
          .Stream(packet)
          .through(Components.retrieveStatePipe)
          .compile
          .last

      for {
        _ <- redis.use(_.hSet(hashKey, FeedState.toRedisHash(expectedState)))
        resOpt <- compiledStream
      } yield resOpt should contain(expectedState -> packet)
    }
  }

  "the save stae pipe" - {
    "can save the feed state to redis" in withRedis { implicit redis =>
      val expectedState = FeedState(2, None)
      val feedId = UUID.randomUUID()
      val packet = Packet[Int](2, "football", "actions", feedId, 0)
      val hashKey = s"${packet.sport}:${packet.feedType}:${packet.feedId}:state"

      val compiledStream =
        fs2
          .Stream(expectedState -> packet)
          .through(Components.saveStatePipe)
          .compile
          .last

      for {
        existsBefore <- redis.use(_.exists(hashKey))
        _ <- compiledStream
        readState <- redis.use(_.hGetAll(hashKey)).map(FeedState.fromRedisHash)
      } yield {
        existsBefore shouldBe false
        readState should contain(expectedState)
      }
    }
  }

}
