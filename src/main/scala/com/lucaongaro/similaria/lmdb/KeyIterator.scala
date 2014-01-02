package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._

class KeyIterator(
  tx:    Transaction,
  c:     Cursor,
  start: Entry
) extends Iterator[Entry] {
  var nextEntry = start

  def next = {
    val result = nextEntry
    nextEntry  = c.get( GetOp.NEXT )
    result
  }

  def hasNext = {
    nextEntry != null
  }
}

object KeyIterator {
  def apply(
    tx:    Transaction,
    c:     Cursor,
    bytes: Array[Byte]
  ) = {
    val first = c.seek( SeekOp.KEY, bytes )
    new KeyIterator( tx, c, first )
  }

  def apply(
    tx:    Transaction,
    c:     Cursor
  ) = {
    val first = c.get( GetOp.FIRST )
    new KeyIterator( tx, c, first )
  }
}
