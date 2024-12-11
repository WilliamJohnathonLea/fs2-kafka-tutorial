package com.xebia.config

final case class AppConfig(
  kafkaBootstrapServers: String,
  inboundTopic: String,
  outboundTopic: String,
  redisUri: String
)
