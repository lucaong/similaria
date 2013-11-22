package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class Key( int: Int )

object Key {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val int = ByteBuffer.wrap( bytes ).getInt
      Some( int )
    }
  }

  implicit def keyToBytes( key: Key ) = {
    val bb = ByteBuffer.allocate(4)
    bb.putInt( key.int ).array()
  }
}
