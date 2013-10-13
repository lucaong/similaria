package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._

class DBManager( opts: DBOptions ) {
  private val env = new Env()
  env.setMapSize( opts.size )
  env.setMaxDbs( 2 )
  env.open( opts.path, NOSYNC )

  private val rndDB = env.openDatabase( "co-index", CREATE )
  private val itrDB = env.openDatabase( "co-occur",
    CREATE | DUPSORT | DUPFIXED )

  def getOccurrency(
    item: Long
  ): Long = {
    val key = Key( item )
    rndDB.get( key ) match {
      case Score( c ) => c
      case _          => 0
    }
  }

  def incrementOccurrency(
    item:       Long,
    increment:  Long
  ) {
    transaction( false ) { tx =>
      val key     = Key( item )
      val current = rndDB.get( tx, key ) match {
        case Score( c ) => c
        case _          => 0
      }
      rndDB.put( tx, key, Score( current + increment ) )
    }
  }

  def getCoOccurrencies(
    item:  Long,
    limit: Integer
  ): List[Tuple2[Long, Long]] = {
    withDupIterator( item, itrDB ) { i =>
      i.map { next =>
        ( next.getValue: @unchecked ) match {
          case KeyScore( key, score ) => ( key, score )
        }
      }.take( limit ).toList
    }
  }

  def incrementCoOccurrency(
    itemA:     Long,
    itemB:     Long,
    increment: Long
  ) {
    transaction( false ) { tx =>
      val coKey   = KeyKey( itemA, itemB )
      val current = rndDB.get( tx, coKey ) match {
        case Score( c ) => c
        case _          => 0
      }
      val score = current + increment
      rndDB.put( tx, coKey, Score( score ) )
      itrDB.delete( tx, Key( itemA ), KeyScore( itemB, current ) )
      itrDB.delete( tx, Key( itemB ), KeyScore( itemA, current ) )
      itrDB.put( tx, Key( itemA ), KeyScore( itemB, score ) )
      itrDB.put( tx, Key( itemB ), KeyScore( itemA, score ) )
    }
  }

  def close() {
    env.close()
  }

  // Private methods

  private def withDupIterator[T](
    start:    Long,
    db:       Database,
    readonly: Boolean = true
  )( block: DupIterator => T ): T = {
    transaction( readonly ) { tx =>
      val c        = db.openCursor( tx )
      val iterator = DupIterator( tx, c, Key( start ) )
      try
        block( iterator )
      finally
        c.close()
    }
  }

  private def transaction[T]( readonly: Boolean )(
    block: Transaction => T
  ): T = {
    val tx      = env.createTransaction( readonly )
    var success = false
    try {
      val result = block( tx )
      success = true
      result
    } finally {
      if ( success )
        tx.commit()
      else
        tx.abort()
    }
  }
}
