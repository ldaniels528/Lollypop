package com.lollypop.runtime.instructions.infrastructure

import com.lollypop.language.HelpDoc.{CATEGORY_DATAFRAMES_INFRA, PARADIGM_DECLARATIVE}
import com.lollypop.language._
import com.lollypop.language.models.ExternalTable
import com.lollypop.runtime.DatabaseManagementSystem.createExternalTable
import com.lollypop.runtime.DatabaseObjectConfig.ExternalTableConfig
import com.lollypop.runtime._
import com.lollypop.runtime.devices.TableColumn
import com.lollypop.runtime.devices.TableColumn.implicits.SQLToColumnConversion
import com.lollypop.runtime.instructions.ReferenceInstruction
import com.lollypop.runtime.instructions.infrastructure.CreateExternalTable.ExternalTableDeclaration
import lollypop.io.IOCost

/**
 * create external table statement
 * @param ref         the [[DatabaseObjectRef persistent object reference]]
 * @param table       the given [[ExternalTable table]]
 * @param ifNotExists if true, the operation will not fail when the entity exists
 * @author lawrence.daniels@gmail.com
 */
case class CreateExternalTable(ref: DatabaseObjectRef, table: ExternalTable, ifNotExists: Boolean)
  extends ReferenceInstruction with RuntimeModifiable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, IOCost) = {
    val (sa, ca, declaration) = parseExternalTableDeclaration()
    val cb = ca ++ createExternalTable(ref.toNS, declaration)
    (sa, cb, cb)
  }

  private def parseExternalTableDeclaration()(implicit scope: Scope): (Scope, IOCost, ExternalTableDeclaration) = {
    val conversions = Seq("\\b" -> "\b", "\\n" -> "\n", "\\r" -> "\r", "\\t" -> "\t")
    val converter: String => String = s => conversions.foldLeft(s) { case (str, (from, to)) => str.replace(from, to) }

    def asBoolean: Any => Boolean = {
      case "true" => true
      case "false" => false
      case b: Boolean => b
      case b: java.lang.Boolean => b
      case x => table.options.dieIllegalType(x)
    }

    def asList: Any => List[String] = {
      case a: Array[String] => a.toList
      case a: Array[_] => a.map(asString).toList
      case s: String => List(s)
      case x => table.options.dieIllegalType(x)
    }

    def asString: Any => String = {
      case c: Char => String.valueOf(c)
      case s: String => converter(s)
      case x => table.options.dieIllegalType(x)
    }

    val (sa, ca, params) = table.options.pullDictionary
    (sa, ca, ExternalTableDeclaration(
      columns = table.columns.map(_.toTableColumn),
      ifNotExists = ifNotExists,
      partitions = params.get("partitions").toList flatMap asList,
      config = ExternalTableConfig(
        fieldDelimiter = params.get("field_delimiter") map asString,
        headers = params.get("headers") map asBoolean,
        format = params.get("format") map asString map (_.toLowerCase),
        lineTerminator = params.get("line_delimiter") map asString,
        location = params.get("location") map asString,
        nullValues = params.get("null_values").toList flatMap asList
      )))
  }

  override def toSQL: String = {
    ("create external table" :: (if (ifNotExists) List("if not exists") else Nil) ::: ref.toSQL ::
      table.columns.map(c => c.toSQL).mkString("(", ", ", ")") ::
      "containing" :: table.options.toSQL :: Nil).mkString(" ")
  }

}

object CreateExternalTable extends ModifiableParser with IfNotExists {
  val template: String =
    """|create external table ?%IFNE:exists %L:name ( %P:columns ) containing %e:options
       |""".stripMargin

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "create external table",
    category = CATEGORY_DATAFRAMES_INFRA,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = template,
    description = "Creates an external table",
    example =
      """|create external table if not exists customers (
         |  customer_uid: UUID,
         |  name: String,
         |  address: String,
         |  ingestion_date: Long
         |) containing { format: 'json', location: './datasets/customers/json/', null_values: ['n/a'] }
         |""".stripMargin
  ))

  override def parseModifiable(ts: TokenStream)(implicit compiler: SQLCompiler): Option[CreateExternalTable] = {
    if (understands(ts)) {
      val params = SQLTemplateParams(ts, template)
      Some(CreateExternalTable(ref = params.locations("name"),
        ExternalTable(
          columns = params.parameters.getOrElse("columns", Nil).map(_.toColumn),
          options = params.expressions("options")),
        ifNotExists = params.indicators.get("exists").contains(true)))
    } else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "create external table"

  /**
   * External Table Declaration
   * @param columns     the [[TableColumn table columns]]
   * @param partitions  the partition column names
   * @param config      the [[ExternalTableConfig table configuration]]
   * @param ifNotExists if true, the operation will not fail when the table exists
   */
  case class ExternalTableDeclaration(columns: List[TableColumn],
                                      partitions: List[String],
                                      config: ExternalTableConfig,
                                      ifNotExists: Boolean)

}