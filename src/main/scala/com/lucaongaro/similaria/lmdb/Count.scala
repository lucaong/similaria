package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class Count( value: Int ) {
  def +( increment: Int ) = {
    Count( value + increment )
  }
}

object Count {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val value = ByteBuffer.wrap( bytes ).getInt
      Some( value )
    }
  }

  implicit def countToBytes( count: Count ) = {
    val bb = ByteBuffer.allocate(4)
    bb.putInt( count.value ).array()
  }
}
