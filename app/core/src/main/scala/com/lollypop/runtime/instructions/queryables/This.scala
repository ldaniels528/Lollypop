package com.lollypop.runtime.instructions.queryables

import com.lollypop.language.HelpDoc.{CATEGORY_SCOPE_SESSION, PARADIGM_DECLARATIVE}
import com.lollypop.language._
import com.lollypop.runtime.datatypes.{StringType, TableType}
import com.lollypop.runtime.devices.{RowCollection, TableColumn}
import com.lollypop.runtime.instructions.expressions.TableExpression
import com.lollypop.runtime.{Scope, _}
import lollypop.io.IOCost

/**
 * This (scope variable)
 */
case class This() extends RuntimeQueryable with TableRendering with TableExpression {

  override def execute()(implicit scope: Scope): (Scope, IOCost, RowCollection) = {
    (scope, IOCost.empty, scope.toRowCollection)
  }

  override def toSQL: String = "this"

  override def returnType: TableType = toTableType

  override def toTable(implicit scope: Scope): RowCollection = scope.toRowCollection

  override def toTableType: TableType = TableType(Seq(
    TableColumn(name = "name", `type` = StringType),
    TableColumn(name = "value", `type` = StringType),
    TableColumn(name = "kind", `type` = StringType)
  ))

}

object This extends QueryableParser {
  private val keyword: String = "this"
  private val templateCard: String = keyword

  override def help: List[HelpDoc] = List(HelpDoc(
    name = keyword,
    category = CATEGORY_SCOPE_SESSION,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = templateCard,
    description = "Table representation of the current scope",
    example = "this"
  ))

  override def parseQueryable(ts: TokenStream)(implicit compiler: SQLCompiler): Option[This] = {
    ts.nextIf(templateCard) ==> This()
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is keyword

}
