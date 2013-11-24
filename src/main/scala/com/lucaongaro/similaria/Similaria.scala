package com.lucaongaro.similaria

import com.lucaongaro.similaria._
import com.lucaongaro.similaria.lmdb._
import scala.collection.SortedSet
import java.io.File

class Similaria(
  implicit
  val opts:       Options,
  val similarity: ( Int, Int, Int ) => Double =
    ( a, b, ab ) => ab.toDouble / ( a + b - ab )
) {

  type PrefSet = Set[Int]
  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  /** Adds a preference set */
  def addPreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, 1 )
    prefSet
  }

  /** Appends a subset to an already existing preference set */
  def addToPreferenceSet(
    originalSet: PrefSet,
    setToAdd:    PrefSet
  ) = {
    val set = setToAdd &~ originalSet
    incrementSubset( originalSet, set, 1 )
    originalSet | set
  }

  /** Removes a preference set */
  def removePreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, -1 )
    prefSet
  }

  /** Removes a subset from an existing preference set */
  def removeFromPreferenceSet(
    originalSet: PrefSet,
    setToRemove: PrefSet
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 )
    originalSet &~ set
  }

  /** Returns the nearest neighbors of the given item */
  def findNeighborsOf(
    item:  Int,
    limit: Int = 20
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

  /** Returns the similarity between two items */
  def getSimilarityBetween(
    item:  Int,
    other: Int
  ): Double = {
    val itemCount = dbm.getOccurrency( item )
    if ( itemCount == 0 ) return 0.0

    val otherCount = dbm.getOccurrency( other )
    if ( otherCount == 0 ) return 0.0

    val coCount = dbm.getCoOccurrency( item, other )
    similarity( itemCount, otherCount, coCount )
  }

  /** Mutes an item (so that it is not considered when getting neighbors) */
  def muteItem(
    item: Int
  ) {
    dbm.setMuted( item, true )
  }

  /** Unmutes an item (so that it is considered when getting neighbors) */
  def unmuteItem(
    item: Int
  ) {
    dbm.setMuted( item, false )
  }

  /** Returns statistics on the database */
  def stats = {
    dbm.stats
  }

  /** Graceful shutdown */
  def shutdown() {
    dbm.close()
  }

  // Private methods

  private def incrementSet(
    set:       PrefSet,
    increment: Int
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
    increment:   Int
  ) {
    incrementSet( subset, increment )
    for ( orig <- originalSet; item <- subset ) {
      dbm.incrementCoOccurrency( orig, item, increment )
    }
  }
}
