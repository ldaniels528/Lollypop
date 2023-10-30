package com.lollypop.runtime.instructions.invocables

import com.lollypop.language.models.Expression.implicits.{LifestyleExpressions, LifestyleExpressionsAny}
import com.lollypop.language.models.{CodeBlock, Column}
import com.lollypop.runtime.instructions.VerificationTools
import com.lollypop.runtime.instructions.functions.NamedFunction
import com.lollypop.runtime.{LollypopCompiler, LollypopVM, Scope}
import com.lollypop.util.StringRenderHelper.StringRenderer
import org.scalatest.funspec.AnyFunSpec

class DefineImplicitTest extends AnyFunSpec with VerificationTools {

  describe(classOf[DefineImplicit].getSimpleName) {

    it("should compile virtual methods") {
      val model = LollypopCompiler().compile(
        """|implicit class `java.lang.String` {
           |    def reverseString(value: String) := {
           |        import "java.lang.StringBuilder"
           |    }
           |}""".stripMargin)
      println(model.render)
      assert(model == DefineImplicit(className = "java.lang.String", methods = CodeBlock(
        DefineFunction(function = NamedFunction(
          name = "reverseString",
          params = Seq(Column(name = "value", `type` = "String".ct)),
          code = CodeBlock(Import("java.lang.StringBuilder".v)),
          returnType_? = None))
      )))
    }

    it("should decompile virtual methods") {
      val model = DefineImplicit(className = "java.lang.String", methods = CodeBlock(
        DefineFunction(function = NamedFunction(
          name = "reverseString",
          params = Seq(Column(name = "value", `type` = "String".ct)),
          code = CodeBlock(Import("java.lang.StringBuilder".v)),
          returnType_? = None))
      ))
      assert(model.toSQL ==
        """|implicit class `java.lang.String` {
           |  def reverseString(value: String) := {
           |  import "java.lang.StringBuilder"
           |}
           |}""".stripMargin)
    }

    it("should create virtual methods") {
      val (_, _, result) = LollypopVM.executeSQL(Scope(),
        """|implicit class "java.lang.String" {
           |    def reverseString(value: String) := {
           |        import "java.lang.StringBuilder"
           |        val src = value.toCharArray()
           |        val dest = new StringBuilder(value.length())
           |        val eol = value.length() - 1
           |        var n = 0
           |        while (n <= eol) {
           |          dest.append(src[eol - n])
           |          n += 1
           |        }
           |        dest.toString()
           |    }
           |}
           |
           |"Hello World".reverseString()
           |""".stripMargin)
      assert(result == "dlroW olleH")
    }

  }

}