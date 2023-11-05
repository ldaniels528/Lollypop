package com.lollypop.runtime.instructions.expressions

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_IO, PARADIGM_DECLARATIVE}
import com.lollypop.language.models.{Expression, Queryable}
import com.lollypop.language.{ExpressionParser, HelpDoc, SQLCompiler, SQLTemplateParams, TokenStream}
import com.lollypop.runtime.instructions.expressions.RuntimeExpression.RichExpression
import com.lollypop.runtime.{LollypopVM, Scope}
import com.lollypop.util.OptionHelper.OptionEnrichment
import lollypop.io.IOCost

/**
 * Draws graphical charts
 * @param chart a dictionary containing the chart options
 * @param from  the data source to graph
 * @example {{{
 * graph { shape: "pie" } from @exposure
 * }}}
 */
case class Graph(chart: Expression, from: Queryable) extends RuntimeExpression {

  override def execute()(implicit scope: Scope): (Scope, IOCost, GraphResult) = {
    val (scope1, cost1, rc) = LollypopVM.search(scope, from)
    val dict = chart.asDictionary(scope1) || dieXXXIsNull("Dictionary")
    dict.get("shape").collect { case s: String => s } || dieXXXIsNull("Attribute 'shape'")
    (scope, cost1, GraphResult(options = dict.toMap, data = rc))
  }

  override def toSQL: String = List("graph", chart.wrapSQL, from.toSQL).mkString(" ")

}

object Graph extends ExpressionParser {
  import com.lollypop.util.OptionHelper.implicits.risky._
  private val template = "graph %e:chart %i:source"

  override def help: List[HelpDoc] = List(
    HelpDoc(
      name = "graph",
      category = CATEGORY_DATAFRAMES_IO,
      paradigm = PARADIGM_DECLARATIVE,
      syntax = template,
      featureTitle = "Dataframe Literals",
      description = "Produces graphical charts",
      example =
        """|graph { shape: "ring", title: "Ring Demo" } from (
           |  |------------------|
           |  | exchange | total |
           |  |------------------|
           |  | NASDAQ   |    24 |
           |  | AMEX     |     5 |
           |  | NYSE     |    28 |
           |  | OTCBB    |    32 |
           |  | OTHEROTC |     7 |
           |  |------------------|
           |)
           |""".stripMargin
    ), HelpDoc(
      name = "graph",
      category = CATEGORY_DATAFRAMES_IO,
      paradigm = PARADIGM_DECLARATIVE,
      syntax = template,
      description = "Produces graphical charts",
      example =
        """|chart = { shape: "pie", title: "Member Types of OS" }
           |graph chart from (
           |  select memberType, total: count(*) from (membersOf(OS))
           |  group by memberType
           |)
           |""".stripMargin
    ), HelpDoc(
      name = "graph",
      category = CATEGORY_DATAFRAMES_IO,
      paradigm = PARADIGM_DECLARATIVE,
      syntax = template,
      description = "Produces graphical charts",
      example =
        """|chart = { shape: "scatter", title: "Scatter Demo" }
           |samples = {
           |  import "java.lang.Math"
           |  def series(x) := "Series {{ (x % 2) + 1 }}"
           |  select w, x, y from ([0 to 500]
           |    .map(x => select w: series(x), x, y: x * iff((x % 2) is 0, Math.cos(x), Math.sin(x)))
           |    .toTable())
           |}
           |graph chart from samples
           |""".stripMargin
    ))

  override def parseExpression(ts: TokenStream)(implicit compiler: SQLCompiler): Option[Graph] = {
    if (understands(ts)) {
      val params = SQLTemplateParams(ts, template)
      Option(Graph(chart = params.expressions("chart"), from = params.queryables("source")))
    } else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "graph"

}