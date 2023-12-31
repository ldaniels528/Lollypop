package com.lollypop.runtime.instructions.expressions

import com.lollypop.language._
import com.lollypop.language.models._
import com.lollypop.runtime.datatypes.{AnyType, DataType}
import com.lollypop.runtime.instructions.functions.ArgumentBlock
import com.lollypop.runtime.plastics.Tuples.seqToTuple
import com.lollypop.runtime.{Scope, _}
import lollypop.io.IOCost

/**
 * Represents a collection of arguments
 */
case class TupleLiteral(args: List[Expression]) extends RuntimeExpression with ArgumentBlock with Literal {

  override def execute()(implicit scope: Scope): (Scope, IOCost, Any) = {
    val (scopeA, costA, values) = args.transform(scope)
    (scopeA, costA, seqToTuple(values).orNull)
  }

  override def parameters: List[ParameterLike] = args.map {
    case f@FieldRef(name) => Parameter(f.getNameOrDie, `type` = (if (name == f.getNameOrDie) "Any" else name).ct)
    case x => dieIllegalType(x)
  }

  override def returnType: DataType = AnyType

  override def toSQL: String = args.map(_.toSQL).mkString("(", ", ", ")")

  override def value: Any = args

}

object TupleLiteral extends ExpressionParser {

  def apply(args: Expression*): TupleLiteral = new TupleLiteral(args.toList)

  override def help: List[HelpDoc] = Nil

  override def parseExpression(stream: TokenStream)(implicit compiler: SQLCompiler): Option[Expression] = {
    if (stream nextIf "(") {
      stream.mark()
      var list: List[Expression] = Nil
      while (stream.hasNext && (stream isnt ")")) {
        val expr = compiler.nextExpression(stream) || stream.dieExpectedExpression()
        list = expr :: list
        if (stream isnt ")") stream.expect(",")
      }
      stream.expect(")")

      // is it a quantity or tuple?
      list match {
        case Nil => Some(TupleLiteral())
        case List(quantity) => Some(quantity)
        case tuple => Some(TupleLiteral(tuple.reverse))
      }
    } else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "("
}