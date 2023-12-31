package com.lollypop.runtime.instructions.invocables

import com.lollypop.language.HelpDoc.{CATEGORY_SYSTEM_TOOLS, PARADIGM_FUNCTIONAL}
import com.lollypop.language.models.Instruction
import com.lollypop.language.{HelpDoc, InvokableParser, SQLCompiler, SQLTemplateParams, TokenStream}
import com.lollypop.runtime.ModelStringRenderer.ModelStringRendering
import com.lollypop.runtime.{Scope, _}
import lollypop.io.IOCost

case class Trace(instruction: Instruction) extends RuntimeInvokable {

  override def execute()(implicit scope: Scope): (Scope, IOCost, Any) = {
    val scope1 = scope.withTrace({ (op, scope, result, elapsedTime) =>

      def friendlyType(result: Any): String = Option(result).map(_.getClass.getSimpleName).orNull

      def opCode(instruction: Instruction): String = {
        s"${instruction.getClass.getSimpleName} ${instruction.toSQL}"
      }

      scope.stdErr.println(f"[$elapsedTime%.6fms] ${opCode(op)} ~> ${result.asModelString} <${friendlyType(result)}>")
    })
    val (s, c, r) = instruction.execute(scope1)
    (scope, c, r)
  }

  override def toSQL: String = s"trace ${instruction.toSQL}"
}

object Trace extends InvokableParser {
  val templateCard = "trace %i:instruction"

  override def parseInvokable(ts: TokenStream)(implicit compiler: SQLCompiler): Option[Trace] = {
    if (understands(ts)) {
      val params = SQLTemplateParams(ts, templateCard)
      Some(Trace(params.instructions("instruction")))
    } else None
  }

  override def help: List[HelpDoc] = List(HelpDoc(
    name = "trace",
    category = CATEGORY_SYSTEM_TOOLS,
    paradigm = PARADIGM_FUNCTIONAL,
    syntax = templateCard,
    description = "Executes an instruction",
    example = "trace set x = 1"
  ))

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is "trace"
}
