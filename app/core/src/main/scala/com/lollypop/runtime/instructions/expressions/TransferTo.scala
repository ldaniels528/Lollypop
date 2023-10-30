package com.lollypop.runtime.instructions.expressions

import com.lollypop.implicits.MagicImplicits
import com.lollypop.language.HelpDoc.{CATEGORY_TRANSFORMATION, PARADIGM_DECLARATIVE}
import com.lollypop.language.models.Expression
import com.lollypop.language.{ExpressionChainParser, HelpDoc, SQLCompiler, TokenStream}
import com.lollypop.runtime.Scope
import com.lollypop.runtime.instructions.Streamable.{getInputStream, getOutputStream}
import com.lollypop.runtime.instructions.expressions.TransferTo._symbol
import com.lollypop.util.ResourceHelper.AutoClose
import org.apache.commons.io.IOUtils

import java.io.OutputStream

/**
 * Transfer-To operator
 * @param a the [[Expression source expression]]
 * @param b the [[Expression target expression]]
 * @example {{{
 * import "java.io.File"
 * f = new File("/Users/ldaniels/.lollypoprc")
 * f ===> out
 * }}}
 * @example {{{
 * import "java.lang.Thread"
 * val total = 100
 * val progressBarWidth = 50
 * [0 to total].foreach(i => {
 *    val progress = Int((Double(i) / total) * progressBarWidth)
 *    val progressBar = "[" + ("*" * progress) + (" " * (progressBarWidth - progress)) + "] {{i}}%"
 *    "\r{{progressBar}}" ===> out
 *    Thread.sleep(Long(100)) // Simulate some work being done
 * })
 * }}}
 */
case class TransferTo(a: Expression, b: Expression) extends RuntimeExpression {
  override def evaluate()(implicit scope: Scope): OutputStream = {
    getInputStream(a).use(in =>
      getOutputStream(b) ~> { out =>
        IOUtils.copy(in, out)
        out
      })
  }

  override def toSQL: String = Seq(a.toSQL, _symbol, b.toSQL).mkString(" ")

}

object TransferTo extends ExpressionChainParser {
  private val _symbol = "===>"

  override def help: List[HelpDoc] = List(HelpDoc(
    name = _symbol,
    category = CATEGORY_TRANSFORMATION,
    paradigm = PARADIGM_DECLARATIVE,
    syntax = s"a ${_symbol} b",
    description = "A declarative way to write to OutputStream or Writer resources",
    example =
      """|import "java.io.File"
         |f = new File("./test.json")
         |f ===> stdout
         |""".stripMargin
  ))

  override def parseExpressionChain(stream: TokenStream, host: Expression)(implicit compiler: SQLCompiler): Option[TransferTo] = {
    stream match {
      case ts if ts nextIf _symbol => for (b <- compiler.nextExpression(ts)) yield TransferTo(host, b)
      case _ => None
    }
  }

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is _symbol

}