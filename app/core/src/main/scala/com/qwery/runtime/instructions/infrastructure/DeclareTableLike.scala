package com.qwery.runtime.instructions.infrastructure

import com.qwery.language.models.{Atom, TableModel}
import com.qwery.runtime.datatypes.TableType.TableTypeRefExtensions
import com.qwery.runtime.devices.RowCollectionZoo.createTempTable
import com.qwery.runtime.devices.TableColumn.implicits.TableColumnToSQLColumnConversion
import com.qwery.runtime.{DatabaseObjectRef, Scope, Variable}
import com.qwery.util.ResourceHelper.AutoClose
import qwery.io.IOCost

import scala.collection.mutable

/**
 * declare table ... like statement
 * @param ref         the given [[Atom database object reference]]
 * @param tableModel  the given [[TableModel table]]
 * @param template    the source [[DatabaseObjectRef reference]]
 * @param ifNotExists if true, the operation will not fail when the entity exists
 * @author lawrence.daniels@gmail.com
 */
case class DeclareTableLike(ref: Atom, tableModel: TableModel, template: DatabaseObjectRef, ifNotExists: Boolean)
  extends RuntimeModifiable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, IOCost) = {
    val tableColumnNames = tableModel.columns.map(_.name).toSet
    val templateColumns = scope.getRowCollection(template).use(_.columns.filterNot(c => tableColumnNames.contains(c.name)))
    val aggColumns = tableModel.columns ::: templateColumns.map(_.toColumn).toList
    val _type = tableModel.copy(columns = aggColumns).toTableType
    val out = createTempTable(_type.columns)
    (scope.withVariable(Variable(name = ref.name, _type, initialValue = out)), IOCost.empty, IOCost.empty)
  }

  override def toSQL: String = {
    val sb = new mutable.StringBuilder("declare table")
    if (ifNotExists) sb.append(" if not exists")
    sb.append(s" ${ref.toSQL} like ${template.toSQL} (${tableModel.columns.map(_.toSQL).mkString(",")})")
    sb.toString()
  }

}

