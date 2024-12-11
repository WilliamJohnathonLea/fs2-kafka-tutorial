package com.xebia.kafka

import cats.effect.kernel.Resource
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO

import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import com.dimafeng.testcontainers.WireMockContainer
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import com.xebia.enrichment.UriProvider

import java.util.UUID

class HttpComponentSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with TestContainerForEach {

  override val containerDef =
    WireMockContainer.Def().withMappingFromResource("enrichment_success.json")

  implicit val client: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  "the enrichment component" - {
    "can add enrichment data to a feed" in withContainers { wm =>
      implicit val uriProvider: UriProvider =
        new WiremockUriProvider(wm.getBaseUrl)
      val state = FeedState.empty
      val packet =
        Packet[Int](
          1,
          "football",
          "actions",
          UUID.fromString("663e9c9c-4fb1-4078-9c99-5ff68250c9f7"),
          0
        )

      val expectedData = EnrichmentData(
        players = Map(
          UUID.fromString("c8f5f7ec-5c2e-4b91-8f14-3724e90577c2") -> "James",
          UUID.fromString("a3e5e7cc-293d-4b12-9f3b-68c3c988762f") -> "Michael",
          UUID.fromString("f4c7a854-234d-47db-93a7-e8a34d5f56c2") -> "William",
          UUID.fromString("e7fa57a6-72c9-4428-a9d7-8ad01ccf1ef2") -> "David",
          UUID.fromString("c1b96e92-cc2b-44d8-8149-c5f0c98fcb8f") -> "John",
          UUID.fromString("a67d4e2e-e38b-4881-9f5e-928b07d18cb1") -> "Robert",
          UUID.fromString("b7fce9b6-7c44-495f-9aef-bc31ce46d7b9") -> "Thomas",
          UUID.fromString("d0a3e687-e317-4539-85f8-4b18a3f65d6c") -> "Charles",
          UUID.fromString("f3e6be84-5d2a-47cc-8a33-e0f738b3cc89") -> "Joseph",
          UUID.fromString("e89f7fa1-fd5b-45d6-8b0a-7ae2dc1c55b6") -> "Daniel",
          UUID.fromString("b28c6f7a-5b9c-4c13-a29f-c71f6b4a6bc8") -> "Andrew"
        ),
        teams = Map(
          UUID.fromString("5f9b49aa-f1bf-4be9-a798-a5bd4e7898fa") -> "Team A",
          UUID.fromString("e61cca15-2952-4ce4-9c79-e08ac1399193") -> "Team B"
        )
      )

      val stream =
        fs2
          .Stream(state -> packet)
          .through(Components.enrichPipe)
          .map(_._1.enrichmentData)
          .compile
          .lastOrError

      stream.map { res =>
        res should contain(expectedData)
      }
    }

    "return empty nothing on failure" in withContainers { wm =>
      implicit val uriProvider: UriProvider =
        new WiremockUriProvider(wm.getBaseUrl)
      val state = FeedState.empty
      val packet = Packet[Int](1, "football", "actions", UUID.randomUUID(), 0)

      val stream =
        fs2
          .Stream(state -> packet)
          .through(Components.enrichPipe)
          .map(_._1.enrichmentData)
          .compile
          .lastOrError

      stream.map { res =>
        res shouldBe empty
      }
    }
  }

}
