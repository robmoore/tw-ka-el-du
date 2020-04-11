package org.sdf.rkm

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.client.create
import org.elasticsearch.client.indexRepository
import org.elasticsearch.common.xcontent.XContentType
import java.time.Duration
import java.util.*

private fun createConsumer(): Consumer<Long, String> {
    val props = Properties()
    props["bootstrap.servers"] = "localhost:9092"
    props["group.id"] = "tweets-processor"
    props["key.deserializer"] = LongDeserializer::class.java
    props["value.deserializer"] = StringDeserializer::class.java
    return KafkaConsumer(props)
}

private val logger = KotlinLogging.logger {}

val esClient = create(host = "localhost", port = 9200)

// create a Repository
// use the default jackson model reader and writer (you can customize)
// opt in to refreshes (we don't want this in production code) so we can test
val tweetRepository = esClient.indexRepository<Tweet>(
        "tweets", refreshAllowed = true).apply {
    deleteIndex()
    createIndex {
        source("""
{
    "settings": {
        "index": {
          "number_of_replicas": 0
        }
    },
    "mappings": {
        "properties": {
          "id": {
            "type": "long"
          },
          "createdAt": {
            "type": "date"
          },
          "text": {
            "type": "text"
          },
          "lang": {
            "type": "text"
          }
        }
    }
}""", XContentType.JSON)
    }
}

private fun consume() {
    val consumer = createConsumer()
    consumer.subscribe(listOf("tweetsTopic"))
    try {
        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            if (records.isEmpty)
                break
            records.iterator().forEach {
                val tweetJson = it.value()
                val tweet = jsonMapper.readValue(tweetJson, Tweet::class.java)
                logger.debug { "Received: $tweet.text" }
                try {
                    tweetRepository.index(tweet.id.toString(), tweet)
                } catch (e: ElasticsearchStatusException) {
                    logger.warn(e) { "Could not index $tweet" }
                }
            }
        }
    } finally {
        consumer.close()
        tweetRepository.refresh()
        esClient.close()
    }
}

fun main() {
    consume()
}

/*
1. Write to Kafka using Json serializer
2. Read from Kafka using Json deserializer
3. Create a Data class for the Twitter message
4. Write data class to Elasticsearch
https://github.com/sksamuel/avro4k
https://github.com/sksamuel/avro4k/issues/1
https://github.com/thake/avro4k-kafka-serializer

https://www.confluent.io/confirmation-docker
 */
