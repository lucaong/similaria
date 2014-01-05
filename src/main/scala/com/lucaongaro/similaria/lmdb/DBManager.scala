package com.lucaongaro.similaria.lmdb

import org.fusesource.lmdbjni._
import org.fusesource.lmdbjni.Constants._
import scala.concurrent.stm._

class DBManager( dbPath: String, dbSize: Long ) {

  class InvalidDataPointException extends Exception

  private val env = new Env()
  env.setMapSize( dbSize )
  env.setMaxDbs( 3 )
  env.open( dbPath, NOSYNC | WRITEMAP )

  // In-memory cache for occurrency counts
  private val cache = TMap.empty[Int, Array[Byte]].single

  // rndDB is optimized for random access on co-occurrencies
  // itrDB is optimized for iterating through co-occurrencies
  // occDB is optimized for random access to occurrencies, and cached in memory
  private val rndDB = env.openDatabase( "co-index", CREATE )
  private val occDB = env.openDatabase( "occur", CREATE )
  private val itrDB = env.openDatabase( "co-occur",
    CREATE | DUPSORT | DUPFIXED )

  /** Returns the occurrency count for the given item */
  def getOccurrency(
    item: Int
  ): Int = atomic { implicit tnx =>
    val key = Key( item )
    cache.getOrElseUpdate( item, occDB.get( key ) ) match {
      case CountMuted( c, _ ) => c
      case _                  => 0
    }
  }

  /** Returns the occurrency count unless the item is muted */
  def getOccurrencyUnlessMuted(
    item: Int
  ): Option[Int] = atomic { implicit tnx =>
    cache.getOrElseUpdate( item, occDB.get( Key( item ) ) ) match {
      case CountMuted( c, false ) => Some( c )
      case CountMuted( c, true )  => None
      case _                      => Some( 0 )
    }
  }

  /** Increments (or decrements) the occurrency count for the given item */
  def incrementOccurrency(
    item:      Int,
    increment: Int
  ) {
    transaction( false ) { tx =>
      val key     = Key( item )
      val updated = occDB.get( tx, key ) match {
        case CountMuted( c, a ) => CountMuted( c + increment, a )
        case _                  => CountMuted( increment, false )
      }
      if ( updated.count > 0 )
        occDB.put( tx, key, updated )
      else
        occDB.delete( tx, key )
      cache.remove( item )
    }
  }

  /** Returns the co-occurrency count for the given pair of items */
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

  /** Returns a list of the items that occur most frequently with the given
    * item, along with their relative co-occurrency and occurrency count,
    * filtering only active items. */
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

  /** Increments (or decrements) the co-occurrency count for the given
    * pair of items */
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

  /** Mutes/unmutes the given item */
  def setMuted(
    item:  Int,
    muted: Boolean
  ) {
    transaction( false ) { tx =>
      val key = Key( item )
      occDB.get( tx, key ) match {
        case CountMuted( c, _ ) =>
          val updated = CountMuted( c, muted )
          occDB.put( tx, key, updated )
          cache.remove( item )
        case _                  => // no-op
      }
    }
  }

  /** Returns statistics for the database */
  def stats = {
    Map(
      "rndDB" -> statsFor( rndDB ),
      "itrDB" -> statsFor( itrDB ),
      "occDB" -> statsFor( occDB )
    )
  }

  /** Makes a copy of the current state of the database in another location */
  def copy( path: String ) {
    env.copy( path )
  }

  /** Closes database */
  def close() {
    env.close()
  }

  /** Pre-heat in-memory cache of occurrencies */
  def preheatCache() {
    withOccurrencyIterator { iterator =>
      iterator.foreach { occ =>
        occ.getKey match {
          case Key( item ) =>
            cache.getOrElseUpdate( item, occ.getValue )
          case _ =>
        }
      }
    }
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

  private def withOccurrencyIterator[T](
    block: KeyIterator => T
  ): T = {
    transaction( true ) { tx =>
      val c        = occDB.openCursor( tx )
      val iterator = KeyIterator( tx, c )
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
    iterator: DupIterator
  ) = {
    iterator.map { dup =>
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
    Map(
      "pageSize"      -> stat.ms_psize,
      "depth"         -> stat.ms_depth,
      "branchPages"   -> stat.ms_branch_pages,
      "leafPages"     -> stat.ms_leaf_pages,
      "overflowPages" -> stat.ms_overflow_pages,
      "entries"       -> stat.ms_entries
    )
  }
}
