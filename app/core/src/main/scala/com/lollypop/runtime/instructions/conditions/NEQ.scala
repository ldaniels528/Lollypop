package com.lollypop.runtime.instructions.conditions

import com.lollypop.language.models.Expression
import com.lollypop.language.{ExpressionToConditionPostParser, HelpDoc, SQLCompiler, TokenStream}
import com.lollypop.runtime.{Scope, _}
import lollypop.io.IOCost

/**
 * SQL: `a` is not equal to `b`
 * @param a the left-side [[Expression expression]]
 * @param b the right-side [[Expression expression]]
 */
case class NEQ(a: Expression, b: Expression) extends AbstractNotEquals {

  override def operator: String = "!="

}

object NEQ extends ExpressionToConditionPostParser {

  override def help: List[HelpDoc] = Nil

  override def parseConditionChain(ts: TokenStream, host: Expression)(implicit compiler: SQLCompiler): Option[NEQ] = {
    if (ts nextIf "!=") compiler.nextExpression(ts).map(NEQ(host, _)) else None
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "!="

}