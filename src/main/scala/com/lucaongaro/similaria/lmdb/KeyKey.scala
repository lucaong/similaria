package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class KeyKey(
  keyA: Long,
  keyB: Long
)

object KeyKey {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val keyA = ByteBuffer.wrap( bytes.take(8) ).getLong
      val keyB = ByteBuffer.wrap( bytes.drop(8) ).getLong
      Some( (keyA, keyB) )
    }
  }

  implicit def keyKeyToBytes( keyKey: KeyKey ) = {
    val bb = ByteBuffer.allocate(16)
    ( keyKey.keyA :: keyKey.keyB :: Nil ).sorted match {
      case List( low, high ) =>
        bb.putLong( low ).putLong( high ).array()
    }
  }
}
