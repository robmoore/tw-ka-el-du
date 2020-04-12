package org.sdf.rkm

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.create
import org.elasticsearch.client.indexRepository
import org.elasticsearch.common.xcontent.XContentType
import java.time.Duration
import java.util.*

private fun createConsumer(): Consumer<Long, String> {
    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
    props[ConsumerConfig.GROUP_ID_CONFIG] = "tweets-processor"
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.java
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"

    return KafkaConsumer(props)
}

private val logger = KotlinLogging.logger {}

private fun createEsClient() = create(host = "localhost", port = 9200)

private fun createTweetRepository(esClient: RestHighLevelClient) = esClient.indexRepository<Tweet>("tweets").apply {
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
    val esClient = createEsClient()
    val tweetRepository = createTweetRepository(esClient)
    val consumer = createConsumer()
    consumer.subscribe(listOf("tweetsTopic"))

    try {
        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            if (records.isEmpty)
                break

            tweetRepository.bulk {
                records.iterator().forEach {
                    val tweet = jsonMapper.readValue(it.value(), Tweet::class.java)
                    logger.debug { "Received: $tweet" }
                    index(tweet.id.toString(), tweet)
                }
            }
            consumer.commitSync()
        }
    } finally {
        consumer.close()
        esClient.close()
    }
}

fun main() {
    consume()
}
