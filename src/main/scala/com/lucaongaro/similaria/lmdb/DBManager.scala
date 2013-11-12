package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._

class DBManager( dbPath: String, dbSize: Long ) {

  class InvalidDataPointException( msg: String ) extends Exception( msg )

  private val env = new Env()
  env.setMapSize( dbSize )
  env.setMaxDbs( 2 )
  env.open( dbPath, NOSYNC | WRITEMAP )

  // rndDB is optimized for random access
  // itrDB is optimized for iterating through co-occurrencies
  private val rndDB = env.openDatabase( "co-index", CREATE )
  private val itrDB = env.openDatabase( "co-occur",
    CREATE | DUPSORT | DUPFIXED )

  // Get occurrency count for an item
  def getOccurrency(
    item: Long
  ): Long = {
    val key = Key( item )
    rndDB.get( key ) match {
      case Count( c ) => c
      case _          => 0
    }
  }

  // Increment (or decrement) occurrency count for an item
  def incrementOccurrency(
    item:       Long,
    increment:  Long
  ) {
    transaction( false ) { tx =>
      val key     = Key( item )
      val updated = rndDB.get( tx, key ) match {
        case Count( c ) => c + increment
        case _          => increment
      }
      if ( updated > 0 )
        rndDB.put( tx, key, Count( updated ) )
      else
        rndDB.delete( tx, key )
    }
  }

  // Get co-occurrency count of a pair of items
  def getCoOccurrency(
    item:  Long,
    other: Long
  ): Long = {
    val coKey = KeyKey( item, other )
    rndDB.get( coKey ) match {
      case Count( c ) => c
      case _          => 0
    }
  }

  // Get the items that occur most frequently with their
  // given item, with their relative co-occurrency count
  def getCoOccurrencies(
    item:  Long,
    limit: Integer = -1
  ): List[(Long, Long, Long)] = {
    withDupIterator( item, itrDB ) { i =>
      val tuples = i.map { next =>
        ( next.getValue ) match {
          case KeyCount( key, count ) =>
            ( key, count, getOccurrency( key ) )
          case _ =>
            throw new InvalidDataPointException(
              s"One co-occurrency of item '$item' is invalid or corrupted")
        }
      }
      if ( limit < 0 )
        tuples.toList
      else
        tuples.take( limit ).toList
    }
  }

  // Increment (or decrement) the co-occurrency count for a pair of items
  def incrementCoOccurrency(
    itemA:     Long,
    itemB:     Long,
    increment: Long
  ) {
    transaction( false ) { tx =>
      val coKey   = KeyKey( itemA, itemB )
      val current = rndDB.get( tx, coKey ) match {
        case Count( c ) => c
        case _          => 0
      }
      val count = current + increment
      itrDB.delete( tx, Key( itemA ), KeyCount( itemB, current ) )
      itrDB.delete( tx, Key( itemB ), KeyCount( itemA, current ) )
      if ( count > 0 ) {
        rndDB.put( tx, coKey, Count( count ) )
        itrDB.put( tx, Key( itemA ), KeyCount( itemB, count ) )
        itrDB.put( tx, Key( itemB ), KeyCount( itemA, count ) )
      } else {
        rndDB.delete( tx, coKey )
      }
    }
  }

  // Get stats for the database
  def stats = {
    Map(
      "rndDB" -> statsFor( rndDB ),
      "itrDB" -> statsFor( itrDB )
    )
  }

  // Hot copy of the current state of the database in another location
  def copy( path: String ) {
    env.copy( path )
  }

  // Close database
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

  private def statsFor( db: Database ) = {
    val stat = db.stat()
    val mask = 0xffffffffL
    Map(
      "pageSize"      -> ( mask & stat.ms_psize ),
      "depth"         -> ( mask & stat.ms_depth ),
      "branchPages"   -> ( mask & stat.ms_branch_pages ),
      "leafPages"     -> ( mask & stat.ms_leaf_pages ),
      "overflowPages" -> ( mask & stat.ms_overflow_pages ),
      "entries"       -> ( mask & stat.ms_entries )
    )
  }
}
