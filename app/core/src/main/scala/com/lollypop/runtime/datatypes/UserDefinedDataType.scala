package com.lollypop.runtime.datatypes

import com.lollypop.language._
import com.lollypop.language.models.ColumnType
import com.lollypop.runtime.{Scope, _}

object UserDefinedDataType extends ColumnTypeParser with DataTypeParser {

  override def getCompatibleType(`class`: Class[_]): Option[DataType] = None

  override def getCompatibleValue(value: Any): Option[DataType] = None

  override def parseDataType(columnType: ColumnType)(implicit scope: Scope): Option[DataType] = {
    columnType.name match {
      case "UDT" => Some(createCustomType(columnType))
      case _ => None
    }
  }

  private def createCustomType(columnType: ColumnType): DataType = {
    try {
      val className = columnType.typeArgs.headOption.getOrElse(columnType.dieMissingClassName())
      Class.forName(className) ~> (_.newInstance()) match {
        case dataType: DataType with ConstructorSupport[_] => dataType
        case other => columnType.dieObjectIsNotADataType(other)
      }
    } catch {
      case e: ClassNotFoundException => columnType.dieTypeMissingDependency(e)
      case e: Exception => columnType.dieTypeLoadFail(e)
    }
  }

  override def parseColumnType(stream: TokenStream)(implicit compiler: SQLCompiler): Option[ColumnType] = {
    if (understands(stream)) {
      val typeName = stream.next().valueAsString
      // is this a class type definition? (e.g. "UDT(com.acme.AnvilTruck)")
      val args = stream.captureIf("(", ")", delimiter = Some(",")) { ts =>
        if (!ts.isQuoted && !ts.isText && !ts.isBackticks) ts.dieExpectedClassType() else ts.next().valueAsString
      }
      args match {
        case List(className) => Option(ColumnType(typeName).copy(typeArgs = Seq(className)))
        case _ => stream.dieExpectedOneArgument()
      }
    } else None
  }

  override def help: List[HelpDoc] = Nil

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = synonyms.exists(ts is _)

  override def synonyms: Set[String] = Set("UDT")

}