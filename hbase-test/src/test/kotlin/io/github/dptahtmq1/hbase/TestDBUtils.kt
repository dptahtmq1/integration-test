package io.github.dptahtmq1.hbase

import io.github.dptahtmq1.common.logger
import org.apache.hadoop.hbase.*
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster
import org.flywaydb.core.Flyway
import java.io.File
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

object TestDBUtils {
    const val PREFIX = "jdbc:phoenix"

    private const val TEST_DIRECTORY = "/tmp/hbase"

    private val log = logger()

    private var zkCluster: MiniZooKeeperCluster? = null
    private var hbaseCluster: MiniHBaseCluster? = null

    fun startHBase(): MiniHBaseCluster? {
        System.setProperty(HBaseCommonTestingUtility.BASE_TEST_DIRECTORY_KEY, TEST_DIRECTORY)
        System.setProperty(HConstants.REGIONSERVER_PORT, "26020")

        val config = HBaseConfiguration.create()
        config.set(HConstants.REGIONSERVER_PORT, "26020")

        val utility = HBaseTestingUtility(config)
        hbaseCluster = utility.startMiniCluster()
        zkCluster = utility.zkCluster

        val flyway = Flyway()
        flyway.setLocations("testdb.migration")
        flyway.setDataSource("$PREFIX:${getPhoenixUrl()}", "", "")
        flyway.migrate()

        return hbaseCluster
    }

    fun getPhoenixUrl() = "localhost:${zkCluster?.clientPort}:/hbase"

    fun shutdown() {
        hbaseCluster?.shutdown()

        val logTempDir = File(TEST_DIRECTORY)
        logTempDir.deleteRecursively()
    }

    fun getData(sql: String, argumentFn: (stmt: PreparedStatement) -> Unit): List<Map<String, Any?>> {
        return DriverManager.getConnection("$PREFIX:${getPhoenixUrl()}").use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                argumentFn(preparedStatement)

                try {
                    val resultSet = preparedStatement.executeQuery()
                    return convertResultSetToList((resultSet))
                } catch (e: Exception) {
                    log.error("Failed to get record from HBase. sql: $sql, error: $e")
                    throw e
                }
            }
        }
    }

    private fun convertResultSetToList(rs: ResultSet): List<Map<String, Any?>> {
        val md = rs.metaData
        val columns = md.columnCount
        val list = ArrayList<Map<String, Any?>>()

        while (rs.next()) {
            val row = mutableMapOf<String, Any?>()
            for (i in 1..columns) {
                row[md.getColumnName(i)] = try {
                    rs.getObject(i)
                } catch (e: Exception) {
                    log.error("rs.getObject(i) is null. Fallback to empty string")
                    ""
                }
            }
            list.add(row)
        }

        return list
    }
}
