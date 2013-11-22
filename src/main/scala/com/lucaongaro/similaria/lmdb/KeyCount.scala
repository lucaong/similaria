package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer
import java.nio.ByteOrder

case class KeyCount(
  key:   Int,
  count: Int
)

object KeyCount {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val le    = ByteOrder.LITTLE_ENDIAN
      val count = ByteBuffer.wrap( bytes.take(4) ).order( le ).getInt * -1
      val key   = ByteBuffer.wrap( bytes.drop(4) ).order( le ).getInt
      Some( (key, count) )
    }
  }

  implicit def keyCountToBytes( keyCount: KeyCount ) = {
    val bb = ByteBuffer.allocate(8)
    bb.order( ByteOrder.LITTLE_ENDIAN )
    bb.putInt( -1 * keyCount.count )
    bb.putInt( keyCount.key ).array()
  }
}
