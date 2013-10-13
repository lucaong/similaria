package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class Key( long: Long )

object Key {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val long = ByteBuffer.wrap( bytes ).getLong
      Some( long )
    }
  }

  implicit def keyToBytes( key: Key ) = {
    val bb = ByteBuffer.allocate(8)
    bb.putLong( key.long ).array()
  }
}
