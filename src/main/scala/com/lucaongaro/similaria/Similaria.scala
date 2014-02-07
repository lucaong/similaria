package com.lucaongaro.similaria

import com.lucaongaro.similaria._
import com.lucaongaro.similaria.lmdb._
import scala.collection.SortedSet
import java.io.File

/** The entry point class in the package, implements a recommendation engine
  * using item-based collaborative filtering
  *
  * Items are represented as integer IDs, and the recommendation engine is
  * trained by submitting sets of items occurring together (a.k.a. preference
  * sets). Similarity between items is given by Jaccard similarity between
  * the sets of preference sets to which items correspond.
  *
  * @constructor creates a new instance of the recommendation engine
  * @param opts the configuration options
  */
class Similaria(
  implicit val opts: Options
) extends ItemBasedRecommender {

  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  def addPreferenceSet(
    prefSet: PreferenceSet,
    count:   Int = 1
  ) = {
    incrementSet( prefSet, count )
    prefSet
  }

  def addToPreferenceSet(
    originalSet: PreferenceSet,
    setToAdd:    PreferenceSet,
    count:       Int = 1
  ) = {
    val set = setToAdd &~ originalSet
    incrementSubset( originalSet, set, count )
    originalSet | set
  }

  def removePreferenceSet(
    prefSet: PreferenceSet,
    count:   Int = 1
  ) = {
    incrementSet( prefSet, -1 * count )
    prefSet
  }

  def removeFromPreferenceSet(
    originalSet: PreferenceSet,
    setToRemove: PreferenceSet,
    count:       Int = 1
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 * count )
    originalSet &~ set
  }

  def findNeighborsOf(
    item:  Int,
    limit: Int = 10
  ): SortedSet[Neighbor] = {
    val itemCount      = dbm.getOccurrency( item )
    val emptySet       = SortedSet.empty[Neighbor]

    dbm.withCoOccurrencyIterator( item ) { coOccurrencies =>
      coOccurrencies.foldLeft( emptySet ) { ( set, coOcc ) =>
        val ( other, coCount, otherCount ) = coOcc
        val maxTheoreticalSim = similarity( itemCount, coCount, coCount )
        if ( set.size >= limit && maxTheoreticalSim < set.last.similarity )
          return set
        val sim = similarity( itemCount, otherCount, coCount )
        ( set + Neighbor( other, sim, coCount ) ).take( limit )
      }
    }
  }

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

  def muteItem(
    item: Int
  ) {
    dbm.setMuted( item, true )
  }

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

  // Jaccard similarity
  private def similarity(
    countA: Int,
    countB: Int,
    coCount: Int
  ): Double = {
    coCount.toDouble / ( countA + countB - coCount )
  }

  private def incrementSet(
    set:       PreferenceSet,
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
    originalSet: PreferenceSet,
    subset:      PreferenceSet,
    increment:   Int
  ) {
    incrementSet( subset, increment )
    for ( orig <- originalSet; item <- subset ) {
      dbm.incrementCoOccurrency( orig, item, increment )
    }
  }
}
