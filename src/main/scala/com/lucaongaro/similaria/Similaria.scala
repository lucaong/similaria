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
) {

  type PrefSet = Set[Int]
  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  /** Adds a preference set
    *
    * @param prefSet the preference set to be added
    * @param count how many times the preference set should be added
    * @return the set that was added
    */
  def addPreferenceSet(
    prefSet: PrefSet,
    count:   Int = 1
  ) = {
    incrementSet( prefSet, count )
    prefSet
  }

  /** Appends a subset to an already existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToAdd the subset to be added
    * @param count how many times the subset should be added
    * @return the resulting set
    */
  def addToPreferenceSet(
    originalSet: PrefSet,
    setToAdd:    PrefSet,
    count:       Int = 1
  ) = {
    val set = setToAdd &~ originalSet
    incrementSubset( originalSet, set, count )
    originalSet | set
  }

  /** Removes a preference set
    *
    * @param prefSet the preference set to be removed
    * @return the set that was removed
    * @param count how many times the preference set should be removed
    */
  def removePreferenceSet(
    prefSet: PrefSet,
    count:   Int = 1
  ) = {
    incrementSet( prefSet, -1 * count )
    prefSet
  }

  /** Removes a subset from an existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToRemove the subset to be removed
    * @param count how many times the subset should be removed
    * @return the resulting set
    */
  def removeFromPreferenceSet(
    originalSet: PrefSet,
    setToRemove: PrefSet,
    count:       Int = 1
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 * count )
    originalSet &~ set
  }

  /** Returns the nearest neighbors of the given item
    *
    * @param item the reference item
    * @param limit the maximum number of neighbors to retrieve
    * @return a sorted set of the nearest neighbors
    */
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

  /** Returns the similarity between two items
    *
    * @param item the first item
    * @param other the other item
    * @return the similarity between item and other
    */
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

  /** Mutes an item (so that it is not considered when getting neighbors)
    *
    * @param item the item to be muted
    */
  def muteItem(
    item: Int
  ) {
    dbm.setMuted( item, true )
  }

  /** Unmutes an item (so that it is considered when getting neighbors)
    *
    * @param item the item to be unmuted
    */
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
