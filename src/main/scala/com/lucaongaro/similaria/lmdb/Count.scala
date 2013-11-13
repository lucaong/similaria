package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class Count( value: Long ) {
  def +( increment: Long ) = {
    Count( value + increment )
  }
}

object Count {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val value = ByteBuffer.wrap( bytes ).getLong
      Some( value )
    }
  }

  implicit def countToBytes( count: Count ) = {
    val bb = ByteBuffer.allocate(8)
    bb.putLong( count.value ).array()
  }
}
