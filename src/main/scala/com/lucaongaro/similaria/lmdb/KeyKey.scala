package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class KeyKey(
  keyA: Int,
  keyB: Int
)

object KeyKey {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val keyA = ByteBuffer.wrap( bytes.take(4) ).getInt
      val keyB = ByteBuffer.wrap( bytes.drop(4) ).getInt
      Some( (keyA, keyB) )
    }
  }

  implicit def keyKeyToBytes( keyKey: KeyKey ) = {
    val bb = ByteBuffer.allocate(8)
    ( keyKey.keyA :: keyKey.keyB :: Nil ).sorted match {
      case List( low, high ) =>
        bb.putInt( low ).putInt( high ).array()
    }
  }
}
