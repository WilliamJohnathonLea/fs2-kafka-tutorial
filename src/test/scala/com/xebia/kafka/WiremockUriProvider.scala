package com.xebia.kafka

import com.xebia.enrichment.UriProvider

class WiremockUriProvider(wmBaseUri: String) extends UriProvider {

  override def provide(sport: String, feedId: String): String =
    s"$wmBaseUri/$sport/$feedId/enrichment"

}
