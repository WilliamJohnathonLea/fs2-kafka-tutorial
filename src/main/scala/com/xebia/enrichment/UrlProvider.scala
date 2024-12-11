package com.xebia.enrichment

trait UriProvider {

  def provide(sport: String, feedId: String): String

}

class LocalUriProvider extends UriProvider {

  override def provide(sport: String, feedId: String): String =
    s"http://localhost:8080/$sport/$feedId/enrichment"

}
