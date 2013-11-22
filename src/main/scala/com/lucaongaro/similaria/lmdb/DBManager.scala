package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._

class DBManager( dbPath: String, dbSize: Long ) {

  class InvalidDataPointException extends Exception

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
    item: Int
  ): Int = {
    val key = Key( item )
    rndDB.get( key ) match {
      case CountMuted( c, _ ) => c
      case _                  => 0
    }
  }

  // Get occurrency count unless the item is muted
  def getOccurrencyUnlessMuted(
    key: Int
  ): Option[Int] = {
    rndDB.get( Key( key ) ) match {
      case CountMuted( c, false ) => Some( c )
      case CountMuted( c, true )  => None
      case _                      => Some( 0 )
    }
  }

  // Increment (or decrement) occurrency count for an item
  def incrementOccurrency(
    item:      Int,
    increment: Int
  ) {
    transaction( false ) { tx =>
      val key     = Key( item )
      val updated = rndDB.get( tx, key ) match {
        case CountMuted( c, a ) => CountMuted( c + increment, a )
        case _                  => CountMuted( increment, false )
      }
      if ( updated.count > 0 )
        rndDB.put( tx, key, updated )
      else
        rndDB.delete( tx, key )
    }
  }

  // Get co-occurrency count of a pair of items
  def getCoOccurrency(
    item:  Int,
    other: Int
  ): Int = {
    val coKey = KeyKey( item, other )
    rndDB.get( coKey ) match {
      case Count( c ) => c
      case _          => 0
    }
  }

  // Get the items that occur most frequently with their
  // given item, with their relative co-occurrency count,
  // filtering only active items.
  def getCoOccurrencies(
    item:  Int,
    limit: Int = -1
  ): List[(Int, Int, Int)] = {
    withDupIterator( item, itrDB ) { i =>
      val tuples = nonMutedCoOccurrencies( i )
      if ( limit < 0 )
        tuples.toList
      else
        tuples.take( limit ).toList
    }
  }

  // Increment (or decrement) the co-occurrency count for a pair of items
  def incrementCoOccurrency(
    itemA:     Int,
    itemB:     Int,
    increment: Int
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

  def setMuted(
    item:   Int,
    active: Boolean
  ) {
    transaction( false ) { tx =>
      val key = Key( item )
      rndDB.get( tx, key ) match {
        case CountMuted( c, _ ) =>
          rndDB.put( tx, key, CountMuted( c, active ) )
        case _                  => // no-op
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
    start:    Int,
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

  private def nonMutedCoOccurrencies(
    iter: DupIterator
  ) = {
    iter.map { dup =>
      dup.getValue match {
        case KeyCount( key, coCount ) =>
          getOccurrencyUnlessMuted( key ) match {
            case Some( count ) => ( key, coCount, count )
            case None          => null
          }
        case _ =>
          throw new InvalidDataPointException
      }
    }.filter( _ != null )
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
