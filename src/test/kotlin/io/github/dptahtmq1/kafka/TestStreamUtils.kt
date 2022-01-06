package io.github.dptahtmq1.kafka

import kafka.server.KafkaConfig
import kafka.server.KafkaServerStartable
import org.apache.curator.test.TestingServer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.io.File
import java.util.*

object TestStreamUtils {
    const val BROKER_HOST = "127.0.0.1"
    const val BROKER_PORT = "9092"

    private const val NODE_ID = "1"
    private const val ZK_PORT = "12181"
    private const val DEFAULT_REPLICATION_FACTOR = 1
    private const val DEFAULT_NUM_PARTITION = 2
    private const val TEMP_DIR = "/tmp"
    private const val KAFKA_DIR = "$TEMP_DIR/kafka"
    private const val ZOOKEEPER_DIR = "$TEMP_DIR/zookeeper"

    private val byteArraySerializerClass: String = ByteArraySerializer::class.java.name
    private var zookeeper: TestingServer? = null
    private var kafka: KafkaServerStartable? = null

    fun startZookeeper(): TestingServer {
        val zookeeperDir = File("$TEMP_DIR/zookeeper")
        return TestingServer(ZK_PORT.toInt(), zookeeperDir)
    }

    fun startKafka(): KafkaServerStartable {
        val properties = Properties()
        properties["broker.id"] = NODE_ID
        properties["port"] = BROKER_PORT
        properties["zookeeper.connect"] = "$BROKER_HOST:$ZK_PORT"
        properties["host.name"] = BROKER_HOST
        properties["offsets.topic.replication.factor"] = DEFAULT_REPLICATION_FACTOR.toString()
        properties["delete.topic.enable"] = true.toString()
        properties["offsets.topic.num.partitions"] = DEFAULT_NUM_PARTITION.toString()
        properties["num.partitions"] = DEFAULT_NUM_PARTITION.toString()
        properties["log.dir"] = KAFKA_DIR

        val kafkaConfig = KafkaConfig(properties)
        val kafkaServer = KafkaServerStartable(kafkaConfig)
        kafkaServer.startup()
        return kafkaServer
    }

    fun stopZookeeper() {
        zookeeper?.stop()

        val tempDir = File(ZOOKEEPER_DIR)
        tempDir.deleteRecursively()
    }

    fun stopKafka() {
        kafka?.shutdown()
        kafka?.awaitShutdown()

        // delete kafka log file every time to clear kafka data
        val logTempDir = File(KAFKA_DIR)
        logTempDir.deleteRecursively()
    }

    fun <K, V> createKafkaConsumer(groupId: String): KafkaConsumer<K, V> {
        val properties = Properties()
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "$BROKER_HOST:$BROKER_PORT")
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

        val byteArrayDeserializerClass: String = ByteArrayDeserializer::class.java.name
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, byteArrayDeserializerClass)
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, byteArrayDeserializerClass)
        return KafkaConsumer<K, V>(properties)
    }

    fun <K, V> createKafkaProducer(): KafkaProducer<K, V> {
        val properties = Properties()
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "$BROKER_HOST:$BROKER_PORT")
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all")
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, "3")
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "16384")
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "5")
        properties.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432")
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, byteArraySerializerClass)
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, byteArraySerializerClass)

        return KafkaProducer<K, V>(properties)
    }
}
