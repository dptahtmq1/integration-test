package io.github.dptahtmq1.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KafkaTest {
    private val topic = "test-topic"

    @BeforeTest
    fun setup() {
        TestStreamUtils.startZookeeper()
        TestStreamUtils.startKafka()
    }

    @Test
    fun `should produce and consume`() {
        // Given
        val kafkaProducer = TestStreamUtils.createKafkaProducer<ByteArray, ByteArray>()
        val kafkaConsumer = TestStreamUtils.createKafkaConsumer<ByteArray, ByteArray>("test")

        val bytes = "test-record".toByteArray(Charsets.UTF_8)

        // When
        kafkaProducer.send(ProducerRecord(topic, bytes))

        // Then
        kafkaConsumer.subscribe(listOf(topic))
        val consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(5))
        val topicRecords = consumerRecords.records(topic)
        assertEquals(1, topicRecords.count())
        assertEquals("test-record", String(topicRecords.first().value()))
    }

    @AfterTest
    fun teardown() {
        TestStreamUtils.stopKafka()
        TestStreamUtils.stopZookeeper()
    }
}