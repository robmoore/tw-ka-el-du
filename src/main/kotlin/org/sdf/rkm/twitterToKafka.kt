package org.sdf.rkm

import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import twitter4j.*
import java.util.*
import kotlin.concurrent.schedule

private fun TwitterStream.addListenerFixed(listener: StatusListener) {
    Twitter4jFixer.addListener(this, listener)
}

private fun createProducer(): Producer<Long, String> {
    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = LongSerializer::class.java.canonicalName
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.canonicalName

    props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
    props[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "snappy"
    props[ProducerConfig.LINGER_MS_CONFIG] = "20"
    props[ProducerConfig.BATCH_SIZE_CONFIG] = "32768"

    return KafkaProducer(props)
}

private val logger = KotlinLogging.logger {}


private fun produce() {
    val twitterStream: TwitterStream = TwitterStreamFactory().instance
    val producer = createProducer()
    twitterStream.addListenerFixed(object : StatusListener {
        override fun onException(e: Exception) {
            logger.error(e) { "Got an exception!" }
        }

        override fun onDeletionNotice(notice: StatusDeletionNotice) {
            logger.info { "Got a status deletion notice id: $notice.statusId" }
        }

        override fun onScrubGeo(userId: Long, upToStatusId: Long) {
            logger.info { "Got scrub_geo event userId:$userId upToStatusId:$upToStatusId" }
        }

        override fun onStallWarning(warning: StallWarning) {
            logger.warn { "Got stall warning: $warning" }
        }

        override fun onStatus(status: Status) {
            logger.debug { status.user.name.toString() + " : " + status.text }
            val futureResult = producer.send(ProducerRecord("tweetsTopic", status.id,
                    jsonMapper.writeValueAsString(Tweet(status.id, status.createdAt, status.text, status.lang))))
            futureResult.get()
        }

        override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
            logger.warn { "Got track limitation notice: $numberOfLimitedStatuses" }
        }
    })
    twitterStream.sample()

    Timer().schedule(60000) {
        twitterStream.cleanUp()
        twitterStream.shutdown()
    }
}

fun main() {
    produce()
}

