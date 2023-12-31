package com.lollypop.runtime.datatypes

import com.lollypop.language.LollypopUniverse
import com.lollypop.runtime.Scope

import java.nio.ByteBuffer.wrap

/**
 * JDBC SQL-XML Type Tests
 */
class SQLXMLTypeTest extends DataTypeFunSpec {
  implicit val ctx: LollypopUniverse = LollypopUniverse()
  implicit val scope: Scope = ctx.createRootScope()

  describe(classOf[SQLXMLType.type].getSimpleName) {

    it("should encode/decode SQLXMLType values") {
      val message = "Hello World".toCharArray
      val blob = SQLXMLType.decode(wrap(SQLXMLType.encode(message)))
      assert(blob.getString == String.valueOf(message))
    }

    it("should resolve 'SQLXML'") {
      verifySpec(spec = "SQLXML", expected = SQLXMLType)
    }

    it("should resolve 'SQLXML[32]'") {
      verifySpec(spec = "SQLXML[32]", expected = ArrayType(SQLXMLType, capacity = Some(32)))
    }

    it("should provide a SQL representation") {
      verifySQL("SQLXML", SQLXMLType)
    }
  }

}