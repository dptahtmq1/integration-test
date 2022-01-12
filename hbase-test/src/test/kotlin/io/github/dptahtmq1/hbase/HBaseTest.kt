package io.github.dptahtmq1.hbase

import io.github.dptahtmq1.common.logger
import org.apache.commons.lang3.exception.ExceptionUtils
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HBaseTest {
    private val log = logger()
    private val upsertQuery = "UPSERT INTO T(A,B) VALUES(?,?)"
    private val selectQuery = "SELECT * FROM T WHERE A = ? FETCH FIRST 1 ROW ONLY"

    @BeforeTest
    fun setup() {
        TestDBUtils.startHBase()
    }

    @Test
    fun `should upsert and read`() {
        // Given
        val key = "test"
        val value = "value"
        insert(key, value)

        // When
        val resultMap = TestDBUtils.getData(selectQuery) {
            it.setString(1, key)
        }

        // Then
        assertEquals(1, resultMap.size)
        val recordMap = resultMap[0]
        assertEquals(key, recordMap["A"])
        assertEquals(value, recordMap["B"])
    }

    private fun insert(key: String, value: String) {
        val phoenixUrl = TestDBUtils.getPhoenixUrl()

        try {
            Class.forName("org.apache.phoenix.jdbc.PhoenixDriver")

            DriverManager.getConnection("${TestDBUtils.PREFIX}:$phoenixUrl").use { conn ->

                conn.prepareStatement(upsertQuery).use { upsertStmt ->
                    upsertStmt.setString(1, key)
                    upsertStmt.setString(2, value)
                    upsertStmt.executeUpdate()
                }

                conn.commit()
            }
        } catch (e: Exception) {
            log.error("CODE 1008: Unable to commit batch: ${ExceptionUtils.getRootCauseMessage(e)}")
        }
    }

    @AfterTest
    fun teardown() {
        TestDBUtils.shutdown()
    }

}