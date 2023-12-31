package com.lollypop.runtime.instructions.infrastructure

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_INFRA, PARADIGM_DECLARATIVE}
import com.lollypop.language._
import com.lollypop.language.models.View
import com.lollypop.runtime.DatabaseManagementSystem.createVirtualTable
import com.lollypop.runtime.instructions.ReferenceInstruction
import com.lollypop.runtime.{DatabaseObjectRef, Scope}
import lollypop.io.IOCost

import scala.collection.mutable

/**
 * create view statement
 * @param ref         the [[DatabaseObjectRef persistent object reference]]
 * @param view        the given [[View view]]
 * @param ifNotExists if true, the operation will not fail when the entity exists
 * @example
 * {{{
 * create view OilAndGas
 * as
 * select Symbol, Name, Sector, Industry, `Summary Quote`
 * from Customers
 * where Industry = 'Oil/Gas Transmission'
 * order by Symbol desc
 * }}}
 * @author lawrence.daniels@gmail.com
 */
case class CreateView(ref: DatabaseObjectRef, view: View, ifNotExists: Boolean)
  extends ReferenceInstruction with RuntimeModifiable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, IOCost) = {
    val cost = createVirtualTable(ref.toNS, view, ifNotExists)
    (scope, cost, cost)
  }

  override def toSQL: String = {
    val sb = new mutable.StringBuilder("create view ")
    if (ifNotExists) sb.append("if not exists ")
    sb.append(s"${ref.toSQL} ")
    sb.append(s":= ${view.query.toSQL}")
    sb.toString()
  }

}

object CreateView extends ModifiableParser with IfNotExists {
  val template = "create view ?%IFNE:exists %L:ref %C(_|:=|as) %Q:query"

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "create view",
    category = CATEGORY_DATAFRAMES_INFRA,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = template,
    description = "Creates a view",
    example =
      """|namespace "temp.temp"
         |drop if exists Students
         |create table Students (name: String(64), grade: Char, ratio: Double) containing (
         ||----------------------------------|
         || name            | grade | ratio  |
         ||----------------------------------|
         || John Wayne      | D     | 0.6042 |
         || Carry Grant     | B     | 0.8908 |
         || Doris Day       | A     | 0.9936 |
         || Audrey Hepburn  | A     | 0.9161 |
         || Gretta Garbeaux | C     | 0.7656 |
         ||----------------------------------|
         |)
         |drop if exists A_Students
         |create view A_Students as select * from Students where ratio >= 0.9
         |ns('A_Students')
         |""".stripMargin
  ))

  override def parseModifiable(ts: TokenStream)(implicit compiler: SQLCompiler): Option[CreateView] = {
    if (understands(ts)) {
      val params = SQLTemplateParams(ts, template)
      Some(CreateView(ref = params.locations("ref"),
        View(query = params.queryables.get("query") || ts.dieExpectedQueryable()),
        ifNotExists = params.indicators.get("exists").contains(true)))
    } else None
  }

  override def understands(stream: TokenStream)(implicit compiler: SQLCompiler): Boolean = stream is "create view"

}