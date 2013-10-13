package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

case class Score( value: Long ) {
  def +( increment: Long ) = {
    Score( value + increment )
  }
}

object Score {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val value = ByteBuffer.wrap( bytes ).getLong
      Some( value )
    }
  }

  implicit def scoreToBytes( score: Score ) = {
    val bb = ByteBuffer.allocate(8)
    bb.putLong( score.value ).array()
  }
}
