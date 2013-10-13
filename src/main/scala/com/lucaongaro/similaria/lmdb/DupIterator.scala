package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._

class DupIterator(
  tx:    Transaction,
  c:     Cursor,
  start: Entry
) extends Iterator[Entry] {
  var nextEntry = start

  def next = {
    val result = nextEntry
    nextEntry  = c.get( GetOp.NEXT_DUP )
    result
  }

  def hasNext = {
    nextEntry != null
  }
}

object DupIterator {
  def apply(
    tx:    Transaction,
    c:     Cursor,
    bytes: Array[Byte]
  ) = {
    val first = c.seek( SeekOp.KEY, bytes )
    new DupIterator( tx, c, first )
  }
}
