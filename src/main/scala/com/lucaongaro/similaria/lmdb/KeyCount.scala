package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer
import java.nio.ByteOrder

case class KeyCount(
  key:   Long,
  count: Long
)

object KeyCount {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val le    = ByteOrder.LITTLE_ENDIAN
      val count = ByteBuffer.wrap( bytes.take(8) ).order( le ).getLong * -1
      val key   = ByteBuffer.wrap( bytes.drop(8) ).order( le ).getLong
      Some( (key, count) )
    }
  }

  implicit def keyCountToBytes( keyCount: KeyCount ) = {
    val bb = ByteBuffer.allocate(16)
    bb.order( ByteOrder.LITTLE_ENDIAN )
    bb.putLong( -1 * keyCount.count )
    bb.putLong( keyCount.key ).array()
  }
}
