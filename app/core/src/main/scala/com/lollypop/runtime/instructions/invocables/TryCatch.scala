package com.lollypop.runtime.instructions.invocables

import com.lollypop.language.HelpDoc.{CATEGORY_CONTROL_FLOW, PARADIGM_FUNCTIONAL}
import com.lollypop.language.models.Expression.implicits.LifestyleExpressionsAny
import com.lollypop.language.models._
import com.lollypop.language.{HelpDoc, InvokableParser, SQLCompiler, SQLTemplateParams, TokenStream}
import com.lollypop.runtime.{LollypopVM, Scope}
import lollypop.io.IOCost

/**
 * Try-Catch-Finally statement
 * @param code      the code to execute
 * @param onError   the error handler
 * @param `finally` the optionally finally clause
 * @example try n /= 0 catch e => stdout <=== e.getMessage() finally n = -1
 */
case class TryCatch(code: Instruction, onError: Instruction, `finally`: Option[Instruction] = None)
  extends RuntimeInvokable with Expression with Queryable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, Any) = {
    try LollypopVM.execute(scope, code) catch {
      case t: Throwable =>
        LollypopVM.execute(scope, onError) match {
          case (s, c, lf: LambdaFunction) => LollypopVM.execute(s, lf.call(List(t.v)))
          case x => x
        }
    } finally {
      `finally`.foreach(LollypopVM.execute(scope, _))
    }
  }

  override def toSQL: String = {
    ("try" :: code.toSQL :: "catch" :: onError.toSQL :: `finally`.toList.flatMap(i => List("finally", i.toSQL))).mkString(" ")
  }
}

object TryCatch extends InvokableParser {
  private[invocables] val templateCard = "try %i:code catch %i:onError ?finally +?%i:finally"

  override def help: List[HelpDoc] = List(
    HelpDoc(
      name = "try",
      category = CATEGORY_CONTROL_FLOW,
      paradigm = PARADIGM_FUNCTIONAL,
      syntax = templateCard,
      description = "Attempts an operation and catches any exceptions that occur preventing them from stopping program execution",
      example =
        """|def connect() := throw new `java.lang.RuntimeException`("Connection error")
           |try connect() catch e => stderr <=== e.getMessage()
           |""".stripMargin
    ), HelpDoc(
      name = "try",
      category = CATEGORY_CONTROL_FLOW,
      paradigm = PARADIGM_FUNCTIONAL,
      syntax = templateCard,
      description = "Attempts an operation and catches any exceptions that occur preventing them from stopping program execution",
      example =
        """|var n = 0
           |try n /= 0 catch e => stderr <=== e.getMessage() finally n = -1
           |this
           |""".stripMargin
    ))

  override def parseInvokable(ts: TokenStream)(implicit compiler: SQLCompiler): Invokable = {
    val p = SQLTemplateParams(ts, templateCard)
    TryCatch(
      code = p.instructions("code"),
      onError = p.instructions("onError"),
      `finally` = p.instructions.get("finally"))
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "try"

}