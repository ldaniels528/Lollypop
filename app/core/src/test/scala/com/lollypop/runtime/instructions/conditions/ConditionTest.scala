package com.lollypop.runtime.instructions.conditions

import com.lollypop.language._
import com.lollypop.language.implicits._
import com.lollypop.runtime.Scope
import lollypop.lang.Null
import org.scalatest.funspec.AnyFunSpec

/**
 * Runtime Condition Test
 */
class ConditionTest extends AnyFunSpec {
  implicit val scope: Scope = Scope()

  describe(classOf[AND].getSimpleName) {

    it("""should negate: AND("a".f === "hello", "b".f === "world")""") {
      assert(AND("a".f === "hello", "b".f === "world").negate == OR("a".f !== "hello", "b".f !== "world"))
    }

    it("""should decompile 'AND("a".f === "hello", "b".f === "world")' to SQL""") {
      assert(AND("a".f is "hello", "b".f is "world").toSQL == """(a is "hello") and (b is "world")""")
    }
  }

  describe(classOf[EQ].getSimpleName) {

    it("""should negate: EQ("hello", "world")""") {
      assert(EQ("hello", "world").negate == NEQ("hello", "world"))
    }
  }

  describe(classOf[GT].getSimpleName) {

    it("""should negate: GT("hello", "world")""") {
      assert(GT("hello", "world").negate == LTE("hello", "world"))
    }
  }

  describe(classOf[GTE].getSimpleName) {

    it("""should negate: GTE("hello", "world")""") {
      assert(GTE("hello", "world").negate == LT("hello", "world"))
    }
  }

  describe(classOf[Isnt].getSimpleName) {
    it("""should negate: IsNotNull("hello")""") {
      assert(Isnt("hello", Null()).negate == Is("hello", Null()))
    }
  }

  describe(classOf[Is].getSimpleName) {
    it("""should negate: IsNull("hello")""") {
      assert(Is("hello", Null()).negate == Isnt("hello", Null()))
    }
  }

  describe(classOf[LT].getSimpleName) {
    it("""should negate: LT("hello", "world")""") {
      assert(LT("hello", "world").negate == GTE("hello", "world"))
    }
  }

  describe(classOf[LTE].getSimpleName) {
    it("""should negate: LTE("hello", "world")""") {
      assert(LTE("hello", "world").negate == GT("hello", "world"))
    }
  }

  describe(classOf[NEQ].getSimpleName) {
    it("""should negate: NEQ("hello", "world")""") {
      assert(NEQ("hello", "world").negate == EQ("hello", "world"))
    }
  }

  describe(classOf[OR].getSimpleName) {
    it("""should negate: OR(a === "hello", b === "world")""") {
      assert(OR("a".f === "hello", "b".f === "world").negate == AND("a".f !== "hello", "b".f !== "world"))
    }
  }

}
