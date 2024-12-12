package com.xebia.kafka

import cats.effect.kernel.Resource
import cats.effect.IO

import dev.profunktor.redis4cats.RedisCommands
import fs2._
import org.http4s.client.Client
import org.http4s.EntityDecoder
import org.http4s.Request
import org.http4s.Uri
import org.typelevel.log4cats.Logger

import com.xebia.enrichment.UriProvider
import com.xebia.model.EnrichmentData
import com.xebia.model.FeedState
import com.xebia.model.Packet

object Components {

  def retrieveStatePipe[P](implicit
    redis: Resource[IO, RedisCommands[IO, String, String]],
    logger: Logger[IO]
  ): Pipe[IO, Packet[P], (FeedState, Packet[P])] =
    _.evalMap { packet =>
      val key = s"${packet.sport}:${packet.feedType}:${packet.feedId}:state"
      logger.info(s"retrieving state for ${packet.sport}:${packet.feedId}") *>
        redis.use { redis =>
          val default = FeedState.empty
          for {
            exists <- redis.exists(key)
            state <- if (!exists)
                       IO.pure(default)
                     else
                       redis
                         .hGetAll(key)
                         .map(hash => FeedState.fromRedisHash(hash).getOrElse(default))
          } yield state -> packet
        }
    }

  def dedupPipe[P](implicit
    logger: Logger[IO]
  ): Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMapFilter { case (state, p) =>
      logger.info(s"deduplicating ${p.sport}:${p.feedId}:${p.seqNum}") *>
        IO.pure {
          Option.when(p.seqNum > state.lastSeqNum) {
            state.copy(lastSeqNum = p.seqNum) -> p
          }
        }
    }

  def enrichPipe[P](implicit
    client: Resource[IO, Client[IO]],
    provider: UriProvider,
    dec: EntityDecoder[IO, EnrichmentData],
    logger: Logger[IO]
  ): Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMap { case (state, p) =>
      val uri = provider.provide(p.sport, p.feedId.toString())
      val req = Request[IO](uri = Uri.unsafeFromString(uri))
      if (state.enrichmentData.nonEmpty) {
        IO.pure(state -> p)
      } else {
        logger.info(s"enriching ${p.sport}:${p.feedId}") *>
          client
            .use(_.expectOption[EnrichmentData](req))
            .recover { case _ => None }
            .map { data =>
              state.copy(enrichmentData = data) -> p
            }
      }
    }

  def transformPipe[PIn, POut](
    fn: (FeedState, Packet[PIn]) => IO[(FeedState, Packet[POut])]
  )(implicit logger: Logger[IO]): Pipe[IO, (FeedState, Packet[PIn]), (FeedState, Packet[POut])] =
    _.evalMap { case (state, pIn) =>
      logger.info(s"transforming ${pIn.sport}:${pIn.feedId}:${pIn.seqNum}") *>
        fn(state, pIn)
    }

  def saveStatePipe[P](implicit
    redis: Resource[IO, RedisCommands[IO, String, String]],
    logger: Logger[IO]
  ): Pipe[IO, (FeedState, Packet[P]), (FeedState, Packet[P])] =
    _.evalMap { case (state, p) =>
      val key = s"${p.sport}:${p.feedType}:${p.feedId}:state"
      logger.info(s"saving state for ${p.sport}:${p.feedId}")
      redis.use { redis =>
        redis.hSet(key, FeedState.toRedisHash(state)).as(state -> p)
      }
    }

}
