package com.lollypop.runtime.instructions.expressions

import com.lollypop.language.models.Expression.implicits.{LifestyleExpressions, RichAliasable}
import com.lollypop.language.models._
import com.lollypop.language.{ExpressionParser, HelpDoc, SQLCompiler, TokenStream}
import com.lollypop.runtime.datatypes.{AnyType, DataType}
import com.lollypop.runtime.instructions.functions.ArgumentBlock
import com.lollypop.runtime.plastics.Tuples.seqToTuple
import com.lollypop.runtime.{LollypopVM, Scope}
import com.lollypop.util.OptionHelper.OptionEnrichment

/**
 * Represents a collection of arguments
 */
case class TupleLiteral(args: List[Expression]) extends RuntimeExpression with ArgumentBlock with Literal {

  override def evaluate()(implicit scope: Scope): Any = {
    val (_, _, values) = LollypopVM.transform(scope, args)
    seqToTuple(values)
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