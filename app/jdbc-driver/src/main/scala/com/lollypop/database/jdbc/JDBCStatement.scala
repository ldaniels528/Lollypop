package com.lollypop.database
package jdbc

import com.lollypop.database.jdbc.types.JDBCWrapper
import com.lollypop.language._
import com.lollypop.runtime._

import scala.beans.{BeanProperty, BooleanBeanProperty}
import scala.collection.mutable.ListBuffer

/**
 * Lollypop Statement
 * @param connection the [[JDBCConnection connection]]
 */
class JDBCStatement(@BeanProperty val connection: JDBCConnection)
  extends java.sql.Statement with JDBCWrapper {
  private val batches = ListBuffer[String]()
  private var _isCloseOnCompletion: Boolean = _

  @BooleanBeanProperty var closed: Boolean = false
  @BeanProperty var cursorName: String = _
  @BeanProperty var escapeProcessing: Boolean = _
  @BeanProperty var fetchDirection: Int = java.sql.ResultSet.FETCH_FORWARD
  @BeanProperty var fetchSize: Int = 100
  @BeanProperty var maxFieldSize: Int = _
  @BeanProperty var maxRows: Int = _
  @BeanProperty var queryTimeout: Int = _
  @BeanProperty var resultSet: java.sql.ResultSet = _
  @BeanProperty val resultSetConcurrency: Int = java.sql.ResultSet.CONCUR_UPDATABLE
  @BeanProperty val resultSetHoldability: Int = java.sql.ResultSet.HOLD_CURSORS_OVER_COMMIT
  @BeanProperty val resultSetType: Int = java.sql.ResultSet.TYPE_SCROLL_SENSITIVE
  @BeanProperty var updateCount: Int = _
  @BooleanBeanProperty var poolable: Boolean = _

  override def cancel(): Unit = ()

  override def clearWarnings(): Unit = ()

  override def close(): Unit = closed = true

  override def addBatch(sql: String): Unit = batches += sql

  override def clearBatch(): Unit = batches.clear()

  override def executeBatch(): Array[Int] = {
    val outcome = (batches map executeUpdate).toArray
    batches.clear()
    outcome
  }

  override def execute(sql: String): Boolean = {
    val outcome = invokeQuery(sql)
    resultSet = JDBCResultSet(connection, outcome)
    updateCount = outcome.getUpdateCount
    outcome.rows.nonEmpty
  }

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = execute(sql)

  override def execute(sql: String, columnIndexes: Array[Int]): Boolean = execute(sql)

  override def execute(sql: String, columnNames: Array[String]): Boolean = execute(sql)

  override def executeQuery(sql: String): java.sql.ResultSet = {
    val outcome = invokeQuery(sql)
    resultSet = JDBCResultSet(connection, outcome)
    updateCount = outcome.getUpdateCount
    resultSet
  }

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = executeUpdate(sql)

  override def executeUpdate(sql: String, columnIndexes: Array[Int]): Int = executeUpdate(sql)

  override def executeUpdate(sql: String, columnNames: Array[String]): Int = executeUpdate(sql)

  override def executeUpdate(sql: String): Int = {
    val outcome = invokeQuery(sql)
    resultSet = JDBCResultSet(connection, outcome)
    updateCount = outcome.getUpdateCount
    updateCount
  }

  override def getGeneratedKeys: java.sql.ResultSet = resultSet

  override def getMoreResults: Boolean = false

  override def getMoreResults(current: Int): Boolean = false

  override def getWarnings: java.sql.SQLWarning = new java.sql.SQLWarning()

  override def closeOnCompletion(): Unit = _isCloseOnCompletion = true

  override def isCloseOnCompletion: Boolean = _isCloseOnCompletion

  private def invokeQuery(sql: String): QueryResponse = {
    connection.client.executeQuery(getDatabase, getSchema, sql, limit = Some(fetchSize))
  }

  protected def getDatabase: String = Option(connection.catalog) || DEFAULT_DATABASE

  protected def getSchema: String = Option(connection.schema) || DEFAULT_SCHEMA

}
