package com.lollypop.runtime.instructions.infrastructure

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_IO, PARADIGM_DECLARATIVE}
import com.lollypop.language._
import com.lollypop.language.models._
import com.lollypop.runtime.devices.RowCollectionZoo.RichDatabaseObjectRef
import com.lollypop.runtime.{DatabaseObjectRef, Scope}
import lollypop.io.IOCost

/**
 * Modifies rows matching a conditional expression from a table
 * @param ref          the [[DatabaseObjectRef reference]] to the table to update
 * @param modification the modification [[ScopeModification instruction]]
 * @param condition    the optional [[Condition condition]]
 * @param limit        the optional [[Expression limit]]
 * @example
 * {{{
 * update stocks#transactions
 * set price = 103.45
 * where symbol is 'GENS'
 * and wherein transactions (transactionTime is "2021-08-05T19:23:12.000Z")
 * limit 20
 * }}}
 * @example
 * {{{
 * update stocks
 * set name = 'Apple, Inc.', sector = 'Technology', industry = 'Computers', lastSale = 203.45
 * where symbol is 'AAPL'
 * limit 20
 * }}}
 * @author lawrence.daniels@gmail.com
 */
case class Update(ref: DatabaseObjectRef, modification: ScopeModification, condition: Option[Condition], limit: Option[Expression])
  extends RuntimeModifiable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, IOCost) = {
    val cost = ref match {
      // is it an inner table reference? (e.g. 'stocks#transactions')
      case st: DatabaseObjectRef if st.isSubTable =>
        st.inside { case (outerTable, innerTableColumn) =>
          outerTable.updateInside(innerTableColumn, modification, condition, limit)
        }
      // must be a standard table reference (e.g. 'stocks')
      case _ => scope.getRowCollection(ref).updateWhere(modification, condition, limit)
    }
    (scope, cost, cost)
  }

  override def toSQL: String = {
    (List("update", ref.toSQL, "set", (" " + modification.toSQL).replace(" set ", "").trim)
      ::: condition.map(i => s"where ${i.toSQL}").toList
      ::: limit.map(i => s"limit ${i.toSQL}").toList
      ).mkString(" ")
  }
}

object Update extends ModifiableParser {
  val template: String = "update %L:name %i:modification ?where +?%c:condition ?limit +?%e:limit"

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "update",
    category = CATEGORY_DATAFRAMES_IO,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = template,
    description = "Modifies rows matching a conditional expression from a table",
    example =
      """|val stocks =
         | |---------------------------------------------------------|
         | | symbol | exchange | lastSale | lastSaleTime             |
         | |---------------------------------------------------------|
         | | ISIT   | NASDAQ   | 189.3509 | 2023-08-05T22:34:20.263Z |
         | | OBEA   | NASDAQ   |  99.1026 | 2023-08-05T22:34:20.279Z |
         | | IJYY   | AMEX     | 190.4665 | 2023-08-05T22:34:20.280Z |
         | | SMPG   | NYSE     | 184.6356 | 2023-08-05T22:34:20.282Z |
         | | UKHT   | NASDAQ   |  71.1514 | 2023-08-05T22:34:20.283Z |
         | |---------------------------------------------------------|
         |update @stocks set lastSaleTime = DateTime() where exchange is "NASDAQ"
         |stocks
         |""".stripMargin
  ), HelpDoc(
    name = "update",
    category = CATEGORY_DATAFRAMES_IO,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = template,
    description = "Modifies rows matching a conditional expression from a table",
    example =
      """|declare table stocks (symbol: String(8), exchange: String(8), transactions: Table (price: Double, transactionTime: DateTime)[5])
         |insert into @stocks (symbol, exchange, transactions)
         |values ('AAPL', 'NASDAQ', {"price":156.39, "transactionTime":"2021-08-05T19:23:11.000Z"}),
         |       ('AMD', 'NASDAQ',  {"price":56.87, "transactionTime":"2021-08-05T19:23:11.000Z"}),
         |       ('INTC','NYSE',    {"price":89.44, "transactionTime":"2021-08-05T19:23:11.000Z"}),
         |       ('AMZN', 'NASDAQ', {"price":988.12, "transactionTime":"2021-08-05T19:23:11.000Z"}),
         |       ('SHMN', 'OTCBB', [{"price":0.0010, "transactionTime":"2021-08-05T19:23:11.000Z"},
         |                          {"price":0.0011, "transactionTime":"2021-08-05T19:23:12.000Z"}])
         |
         |update @stocks#transactions
         |set price = 0.0012
         |where symbol is 'SHMN'
         |and transactions wherein (price is 0.001)
         |limit 1
         |stocks
         |""".stripMargin
  ))

  override def parseModifiable(ts: TokenStream)(implicit compiler: SQLCompiler): Option[Update] = {
    if (understands(ts)) {
      val params = SQLTemplateParams(ts, template)
      Some(Update(
        ref = params.locations("name"),
        modification = params.instructions("modification") match {
          case smi: ScopeModification => smi
          case ins => ts.dieExpectedScopeMutation(ins)
        },
        condition = params.conditions.get("condition"),
        limit = params.expressions.get("limit")))
    } else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "update"

}