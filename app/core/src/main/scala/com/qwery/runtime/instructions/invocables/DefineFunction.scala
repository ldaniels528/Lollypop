package com.qwery.runtime.instructions.invocables

import com.qwery.language.HelpDoc.{CATEGORY_CONTROL_FLOW, PARADIGM_FUNCTIONAL}
import com.qwery.language.models.{Expression, TypicalFunction}
import com.qwery.language.{ExpressionParser, HelpDoc, InvokableParser, SQLCompiler, SQLTemplateParams, TokenStream}
import com.qwery.runtime.instructions.functions.{AnonymousFunction, NamedFunction}
import com.qwery.runtime.{QweryVM, Scope}
import qwery.io.IOCost

/**
 * Represents a define function instruction
 * @param function the [[TypicalFunction function]] to create
 * @example def factorial(n: Double) := iff(n <= 1.0, 1.0, n * factorial(n - 1.0))
 */
case class DefineFunction(function: TypicalFunction) extends RuntimeInvokable with Expression {

  override def execute()(implicit scope: Scope): (Scope, IOCost, Any) = QweryVM.execute(scope, function)

  override def toSQL: String = {
    function match {
      case _: AnonymousFunction => function.toSQL
      case _ => Seq("def", function.toSQL).mkString(" ")
    }
  }

}

/**
 * Parses named functions
 */
object DefineFunction extends ExpressionParser with InvokableParser {
  private val template1 = "%C(key|def|function) %a:name %FP:params ?: +?%T:returnType %C(_|as|:=) %i:code"

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "def",
    category = CATEGORY_CONTROL_FLOW,
    paradigm = PARADIGM_FUNCTIONAL,
    syntax = template1,
    description = "Defines a named user-defined function",
    example =
      """|def ¡(n: Double) := iff(n <= 1.0, 1.0, n * ¡(n - 1.0))
         |
         |¡(5)
         |""".stripMargin
  ), HelpDoc(
    name = "def",
    category = CATEGORY_CONTROL_FLOW,
    paradigm = PARADIGM_FUNCTIONAL,
    syntax = template1,
    description = "Defines a named user-defined function",
    example =
      """|def msec(op) := {
         |    import ["java.lang.System"]
         |    val startTime = System.nanoTime()
         |    val result = op()
         |    val elapsedTime = (System.nanoTime() - startTime) / 1000000.0
         |    (elapsedTime, result)
         |}
         |
         |def ¡(n: Double) := iff(n <= 1.0, 1.0, n * ¡(n - 1.0))
         |
         |msec(() => ¡(6))
         |""".stripMargin
  ), HelpDoc(
    name = "def",
    category = CATEGORY_CONTROL_FLOW,
    paradigm = PARADIGM_FUNCTIONAL,
    syntax = template1,
    description = "Defines a named user-defined function",
    example =
      """|def roman(value: Int) := ("I" * value)
         |  .replaceAll("IIIII", "V")
         |  .replaceAll("IIII", "IV")
         |  .replaceAll("VV", "X")
         |  .replaceAll("VIV", "IX")
         |  .replaceAll("XXXXX", "L")
         |  .replaceAll("XXXX", "XL")
         |  .replaceAll("LL", "C")
         |  .replaceAll("LXL", "XC")
         |  .replaceAll("CCCCC", "D")
         |  .replaceAll("CCCC", "CD")
         |  .replaceAll("DD", "M")
         |  .replaceAll("DCD", "CM")
         |
         |roman(1023)
         |""".stripMargin
  ))

  override def parseExpression(ts: TokenStream)(implicit compiler: SQLCompiler): Option[Expression] = {
    if (understands(ts)) {
      parseInvokable(ts) match {
        case DefineFunction(af: AnonymousFunction) => Some(af)
        case function => Some(function)
      }
    } else None
  }

  override def parseInvokable(ts: TokenStream)(implicit compiler: SQLCompiler): DefineFunction = {
    val params = SQLTemplateParams(ts, template1)
    val function = NamedFunction(
      name = params.atoms("name").name,
      params = params.parameters("params"),
      returnType_? = params.types.get("returnType"),
      code = params.instructions("code"))
    DefineFunction(function)
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "def"

}
