package com.lollypop.runtime.devices

import com.lollypop.runtime.instructions.VerificationTools
import com.lollypop.runtime.{DatabaseObjectRef, LollypopVM, Scope}
import lollypop.io.IOCost
import org.scalatest.funspec.AnyFunSpec

import scala.io.Source

class StatsGeneratingRowCollectionTest extends AnyFunSpec with VerificationTools {
  private val tableRef: DatabaseObjectRef = DatabaseObjectRef(getTestTableName)

  describe(classOf[StatsGeneratingRowCollection].getSimpleName) {

    it("should prepare a sample data table") {
      val (scope0, cost0, _) = LollypopVM.executeSQL(Scope(),
        s"""|drop if exists $tableRef
            |create table $tableRef (
            |   symbol: String(5),
            |   exchange: String(6),
            |   lastSale: Double,
            |   lastSaleTime: DateTime
            |)
            |""".stripMargin)
      assert(cost0 == IOCost(created = 1, destroyed = 1))

      // setup the device and place it in the scope
      val device = StatsGeneratingRowCollection(scope0.getRowCollection(tableRef))
      val scope1 = scope0.withVariable("stocks", device)

      // insert 5,000 records
      val src = Source.fromFile("./app/examples/stocks-5k.csv")
      val lines = src.getLines()
      lines.next()
      lines foreach { line =>
        if (line.trim.nonEmpty) {
          val Array(symbol, exchange, lastSale, lastSaleTime) = line.split("[,]")
          val (_, cost, _) = LollypopVM.executeSQL(scope1,
            s"""|insert into @stocks (symbol, exchange, lastSale, lastSaleTime)
                |values ($symbol, $exchange, $lastSale, $lastSaleTime)
                |""".stripMargin)
          assert(cost.inserted == 1)
        }
      }
      src.close()
    }

  }

}
