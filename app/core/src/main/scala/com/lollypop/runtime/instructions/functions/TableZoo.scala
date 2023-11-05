package com.lollypop.runtime.instructions.functions

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_INFRA, PARADIGM_FUNCTIONAL}
import com.lollypop.language.models.Column
import com.lollypop.runtime.Scope
import com.lollypop.runtime.devices.RowCollectionBuilder
import com.lollypop.runtime.devices.TableColumn.implicits.SQLToColumnConversion
import com.lollypop.runtime.instructions.expressions.RuntimeExpression
import lollypop.io.IOCost

case class TableZoo(columns: List[Column]) extends ScalarFunctionCall with RuntimeExpression {
  override val functionName: String = "TableZoo"

  override def execute()(implicit scope: Scope): (Scope, IOCost, RowCollectionBuilder) = {
    (scope, IOCost.empty, RowCollectionBuilder(columns = columns.map(_.toTableColumn)))
  }

  override def toSQL: String = List(functionName, columns.map(_.toSQL).mkString("(", ", ", ")")).mkString

}

object TableZoo extends FunctionCallParserP(
  name = "TableZoo",
  category = CATEGORY_DATAFRAMES_INFRA,
  paradigm = PARADIGM_FUNCTIONAL,
  description = "Returns a Table builder",
  example =
    """|stocks =
       |  TableZoo(symbol: String(10), exchange: String(10), lastSale: Double, lastSaleTime: DateTime)
       |    .withMemorySupport(150)
       |    .build()
       |insert into @stocks
       ||----------------------------------------------------------|
       || exchange  | symbol | lastSale | lastSaleTime             |
       ||----------------------------------------------------------|
       || OTHER_OTC | MBANF  |   0.0109 | 2023-09-21T04:57:58.702Z |
       || OTHER_OTC | YAMJI  |   0.0155 | 2023-09-21T04:57:24.456Z |
       || OTCBB     | HQCY   |   0.0135 | 2023-09-21T04:57:53.351Z |
       || OTHER_OTC | GEYSG  |   0.0186 | 2023-09-21T04:57:28.014Z |
       || OTHER_OTC | WYISA  |   0.0132 | 2023-09-21T04:57:58.271Z |
       || OTCBB     | TXWFI  |   0.0194 | 2023-09-21T04:58:06.199Z |
       || OTCBB     | ZIYBG  |   0.0167 | 2023-09-21T04:58:03.392Z |
       ||----------------------------------------------------------|
       |stocks
       |""".stripMargin)
