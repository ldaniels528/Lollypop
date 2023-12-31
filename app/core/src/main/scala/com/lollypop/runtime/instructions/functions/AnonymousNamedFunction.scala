package com.lollypop.runtime.instructions.functions

import com.lollypop.language.models.{Expression, LambdaFunction}
import com.lollypop.runtime.instructions.expressions.NamedFunctionCall

/**
 * Facilitates the representation of a named function as an anonymous function
 * @param name the name of the referenced function
 * @example {{{
 * val fx = isNumber
 * fx(8.9)
 * }}}
 */
case class AnonymousNamedFunction(name: String) extends LambdaFunction {

  override def call(args: List[Expression]): NamedFunctionCall = NamedFunctionCall(name, args)

  override def toSQL: String = name

}
