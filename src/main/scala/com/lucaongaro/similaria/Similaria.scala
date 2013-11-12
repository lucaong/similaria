package com.lucaongaro.similaria

import com.lucaongaro.similaria._
import com.lucaongaro.similaria.lmdb._
import scala.collection.SortedSet
import java.io.File

class Similaria(
  implicit
  val opts:       Options,
  val similarity: ( Long, Long, Long ) => Double =
    ( a, b, ab ) => ab.toDouble / ( a + b - ab )
) {

  type PrefSet = Set[Long]
  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  // Add preference set
  def addPreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, 1 )
    prefSet
  }

  // Append subset to an already existing preference set
  def addToPreferenceSet(
    originalSet: PrefSet,
    setToAdd:    PrefSet
  ) = {
    val set = setToAdd &~ originalSet
    incrementSubset( originalSet, set, 1 )
    originalSet | set
  }

  // Remove prefernce set
  def removePreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, -1 )
    prefSet
  }

  // Remove subset from existing preference set
  def removeFromPreferenceSet(
    originalSet: PrefSet,
    setToRemove: PrefSet
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 )
    originalSet &~ set
  }

  // Find the items that are most similar to the given item
  def findNeighborsOf(
    item:  Long,
    limit: Integer = 20
  ) = {
    val n              = if ( limit > 50 ) limit * 2 else 100
    val coOccurrencies = dbm.getCoOccurrencies( item, n )
    val itemCount      = dbm.getOccurrency( item )
    val emptySet       = SortedSet.empty[Neighbor]

    coOccurrencies.foldLeft( emptySet ) { ( set, coOcc ) =>
      val ( other, coCount, otherCount ) = coOcc
      val sim = similarity( itemCount, otherCount, coCount )
      set + Neighbor( other, sim )
    }.take( limit )
  }

  // Get similarity between two items
  def getSimilarityBetween(
    item:  Long,
    other: Long
  ): Double = {
    val itemCount = dbm.getOccurrency( item )
    if ( itemCount == 0 ) return 0.0

    val otherCount = dbm.getOccurrency( other )
    if ( otherCount == 0 ) return 0.0

    val coCount = dbm.getCoOccurrency( item, other )
    similarity( itemCount, otherCount, coCount )
  }

  // Get statistics on the database
  def stats = {
    dbm.stats
  }

  // Graceful shutdown
  def shutdown() {
    dbm.close()
  }

  // Private methods

  private def incrementSet(
    set:       PrefSet,
    increment: Long
  ) {
    set.subsets(2).map( _.toList ).foreach {
      case item1 :: item2 :: Nil =>
        dbm.incrementCoOccurrency( item1, item2, increment )
      case _ =>
    }
    set.foreach { item =>
      dbm.incrementOccurrency( item, increment )
    }
  }

  private def incrementSubset(
    originalSet: PrefSet,
    subset:      PrefSet,
    increment:   Long
  ) {
    incrementSet( subset, increment )
    for ( orig <- originalSet; item <- subset ) {
      dbm.incrementCoOccurrency( orig, item, increment )
    }
  }
}
