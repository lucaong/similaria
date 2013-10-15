package com.lucaongaro.similaria

import com.lucaongaro.similaria._
import com.lucaongaro.similaria.lmdb._
import scala.collection.SortedSet
import java.io.File

class Similaria( implicit val opts: Options ) extends CollaborativeFilter {
  type PrefSet = Set[Long]
  val dbm = new DBManager( opts.dbPath, opts.dbSize )

  def addPreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, 1 )
    prefSet
  }

  def addToPreferenceSet(
    originalSet: PrefSet,
    setToAdd:    PrefSet
  ) = {
    val set = setToAdd -- originalSet
    incrementSubset( originalSet, set, 1 )
    originalSet ++ set
  }

  def removePreferenceSet(
    prefSet: PrefSet
  ) = {
    incrementSet( prefSet, -1 )
    prefSet
  }

  def removeFromPreferenceSet(
    originalSet: PrefSet,
    setToRemove: PrefSet
  ) = {
    val set = setToRemove & originalSet
    incrementSubset( originalSet, set, -1 )
    originalSet -- set
  }

  def findNeighborsOf(
    item:  Long,
    limit: Integer = 20
  ) = {
    val n              = if ( limit > 50 ) limit * 2 else 100
    val coOccurrencies = dbm.getCoOccurrencies( item, n )
    val itemCount      = dbm.getOccurrency( item )
    val emptySet       = SortedSet.empty[Neighbor]

    coOccurrencies.foldLeft( emptySet ) { ( set, coOcc ) =>
      val ( other, coCount ) = coOcc
      val otherCount = dbm.getOccurrency( other )
      val sim = opts.similarity( itemCount, otherCount, coCount )
      set + Neighbor( other, sim )
    }.take( limit )
  }

  def stats = {
    dbm.stats
  }

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
