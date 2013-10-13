package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer
import java.nio.ByteOrder

case class KeyScore(
  key:   Long,
  score: Long
)

object KeyScore {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val le    = ByteOrder.LITTLE_ENDIAN
      val score = ByteBuffer.wrap( bytes.take(8) ).order( le ).getLong * -1
      val key   = ByteBuffer.wrap( bytes.drop(8) ).order( le ).getLong
      Some( (key, score) )
    }
  }

  implicit def keyScoreToBytes( keyScore: KeyScore ) = {
    val bb = ByteBuffer.allocate(16)
    bb.order( ByteOrder.LITTLE_ENDIAN )
    bb.putLong( -1 * keyScore.score )
    bb.putLong( keyScore.key ).array()
  }
}
