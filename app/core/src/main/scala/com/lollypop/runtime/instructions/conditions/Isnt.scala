package com.lollypop.runtime.instructions.conditions

import com.lollypop.language.HelpDoc.{CATEGORY_FILTER_MATCH_OPS, PARADIGM_DECLARATIVE}
import com.lollypop.language.models.{Condition, Expression}
import com.lollypop.language.{ExpressionToConditionPostParser, HelpDoc, SQLCompiler, TokenStream}
import com.lollypop.runtime.instructions.conditions.Isnt.keyword

/**
 * Returns true if the `a` is not exactly `b`
 * @param a the left-side [[Expression expression]]
 * @param b the right-side [[Expression expression]]
 */
case class Isnt(a: Expression, b: Expression) extends AbstractNotEquals {

  override def operator: String = keyword

}

object Isnt extends ExpressionToConditionPostParser {
  private val keyword = "isnt"

  override def parseConditionChain(ts: TokenStream, host: Expression)(implicit compiler: SQLCompiler): Option[Condition] = {
    if (ts.nextIf(keyword)) compiler.nextExpression(ts).map(Isnt(host, _)) else None
  }

  override def help: List[HelpDoc] = List(HelpDoc(
    name = keyword,
    category = CATEGORY_FILTER_MATCH_OPS,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = "`value` isnt `expression`",
    description = "returns true if the `value` is not exactly the `expression`; otherwise false",
    example =
      """|x = 199
         |x isnt 200
         |""".stripMargin
  ), HelpDoc(
    name = keyword,
    category = CATEGORY_FILTER_MATCH_OPS,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = "`value` isnt `expression`",
    description = "returns true if the `value` is not exactly the `expression`; otherwise false",
    example =
      """|x = 200
         |x isnt 200
         |""".stripMargin
  ))

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is keyword

}
