package com.lollypop.runtime.instructions.conditions

import com.lollypop.language.HelpDoc.{CATEGORY_FILTER_MATCH_OPS, PARADIGM_IMPERATIVE}
import com.lollypop.language.models.Expression
import com.lollypop.runtime.instructions.functions.{FunctionCallParserE1, ScalarFunctionCall}
import com.lollypop.runtime.{LollypopVM, Scope}

/**
 * SQL: `expression` is null
 * @param expr the [[Expression expression]] to evaluate
 */
case class IsNull(expr: Expression) extends ScalarFunctionCall with RuntimeCondition {
  override def isTrue(implicit scope: Scope): Boolean = LollypopVM.execute(scope, expr)._3 == null

  override def toSQL: String = s"${expr.toSQL} is null"
}

object IsNull extends FunctionCallParserE1(
  name = "isNull",
  category = CATEGORY_FILTER_MATCH_OPS,
  paradigm = PARADIGM_IMPERATIVE,
  description = "Returns true if the expression is null, otherwise false.",
  example = "isNull(null)")