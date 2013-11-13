package com.lucaongaro.similaria.lmdb

import scala.language.implicitConversions
import java.nio.ByteBuffer

// A class to serialize/deserialize occurrency counts with an active flag.
// Since counts can only be positive, the sign is used for the active flag:
// positive means active, negative means inactive.
case class CountMuted(
  count:   Long,
  isMuted: Boolean
) {
  def +( increment: Long ) = {
    CountMuted( count + increment, isMuted )
  }
}

object CountMuted {
  def unapply( bytes: Array[Byte] ) = {
    if ( bytes == null )
      None
    else {
      val value = ByteBuffer.wrap( bytes ).getLong
      Some( (value.abs, value < 0) )
    }
  }

  implicit def countMutedToBytes( ca: CountMuted ) = {
    val bb = ByteBuffer.allocate(8)
    val s  = if ( ca.isMuted ) -1 else 1
    bb.putLong( ca.count * s ).array()
  }
}
