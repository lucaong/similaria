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
  * sets)
  *
  * @constructor creates a new instance of the recommendation engine
  * @param opts the configuration options
  * @param similarity the measure of similarity between two items, which is a
  *        function receiving the occurrency count of each of the two items
  *        and their co-occurrency count. The default is Jaccard similarity
  */
class Similaria(
  implicit
  val opts:       Options,
  val similarity: ( Int, Int, Int ) => Double =
    ( a, b, ab ) => ab.toDouble / ( a + b - ab )
) {

  type PrefSet = Set[Int]
  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  /** Adds a preference set
    *
    * @param prefSet the preference set to be added
    * @return the set that was added
    */
  def addPreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, 1 )
    prefSet
  }

  /** Appends a subset to an already existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToAdd the subset to be added
    * @return the resulting set
    */
  def addToPreferenceSet(
    originalSet: PrefSet,
    setToAdd:    PrefSet
  ) = {
    val set = setToAdd &~ originalSet
    incrementSubset( originalSet, set, 1 )
    originalSet | set
  }

  /** Removes a preference set
    *
    * @param prefSet the preference set to be removed
    * @return the set that was removed
    */
  def removePreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, -1 )
    prefSet
  }

  /** Removes a subset from an existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToRemove the subset to be removed
    * @return the resulting set
    */
  def removeFromPreferenceSet(
    originalSet: PrefSet,
    setToRemove: PrefSet
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 )
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
    limit: Int = 20
  ) = {
    val n              = if ( limit > 25 ) limit * 2 else 50
    val coOccurrencies = dbm.getCoOccurrencies( item, n )
    val itemCount      = dbm.getOccurrency( item )
    val emptySet       = SortedSet.empty[Neighbor]

    coOccurrencies.foldLeft( emptySet ) { ( set, coOcc ) =>
      val ( other, coCount, otherCount ) = coOcc
      val sim = similarity( itemCount, otherCount, coCount )
      set + Neighbor( other, sim, coCount )
    }.take( limit )
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
