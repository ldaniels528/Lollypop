package com.qwery.runtime.instructions.infrastructure

import com.qwery.language.HelpDoc.{CATEGORY_DATAFRAME, PARADIGM_DECLARATIVE}
import com.qwery.language._
import com.qwery.language.models.View
import com.qwery.runtime.DatabaseManagementSystem.createVirtualTable
import com.qwery.runtime.{DatabaseObjectRef, Scope}
import com.qwery.util.OptionHelper.OptionEnrichment
import qwery.io.IOCost

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
case class CreateView(ref: DatabaseObjectRef, view: View, ifNotExists: Boolean) extends RuntimeModifiable {

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
    category = CATEGORY_DATAFRAME,
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

  override def parseModifiable(ts: TokenStream)(implicit compiler: SQLCompiler): CreateView = {
    val params = SQLTemplateParams(ts, template)
    CreateView(ref = params.locations("ref"),
      View(query = params.queryables.get("query") || ts.dieExpectedQueryable()),
      ifNotExists = params.indicators.get("exists").contains(true))
  }

  override def understands(stream: TokenStream)(implicit compiler: SQLCompiler): Boolean = stream is "create view"

}