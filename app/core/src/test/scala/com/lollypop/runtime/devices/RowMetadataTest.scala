package com.lollypop.runtime.devices

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.slf4j.LoggerFactory

/**
 * Row Metadata Test Suite
 */
class RowMetadataTest extends AnyFunSpec {
  private val logger = LoggerFactory.getLogger(getClass)

  describe(classOf[RowMetadata].getSimpleName) {

    it("should encode/decode every permutation of metadata") {
      for {
        a <- Seq(true, false)
        b <- Seq(true, false)
        e <- Seq(true, false)
        r <- Seq(true, false)
      } yield {
        verify(RowMetadata(isAllocated = a, isBlob = b, isEncrypted = e, isReplicated = r))
      }
    }
  }

  private def verify(md: RowMetadata): Assertion = {
    val code = md.encode
    logger.info(f"$md ~> [${code & 0xFF}%02x] ${(code & 0xFF).toBinaryString}")
    assert(RowMetadata.decode(code) == md)
  }

}
