package com.qwery.runtime.instructions.invocables

import com.qwery.language.models.CodeBlock.summarize
import com.qwery.language.models.{CodeBlock, Instruction}
import com.qwery.runtime.Scope
import qwery.io.IOCost

/**
 * Represents an chained code block (e.g. "set a = 1 && set b = 2 && set c = 3")
 * @param instructions one or more [[Instruction instructions]] to execute
 */
case class ChainedCodeBlock(instructions: List[Instruction]) extends CodeBlock with RuntimeInvokable {
  override def execute()(implicit scope: Scope): (Scope, IOCost, Any) = summarize(scope, instructions)

  override def toSQL: String = instructions.map(_.toSQL).mkString(" && ")
}

/**
 * Chained Code Block Companion
 * @author lawrence.daniels@gmail.com
 */
object ChainedCodeBlock {
  def apply(instructions: Instruction*) = new ChainedCodeBlock(instructions.toList)
}
