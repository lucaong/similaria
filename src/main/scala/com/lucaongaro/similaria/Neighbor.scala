package com.lucaongaro.similaria

/** The representation of an item as a neighbor of another reference item
  *
  * @constructor creates a new neighbor
  * @param item the neighbor item
  * @param similarity the similarity score to the reference item
  * @param coOccurrencies the count of co-occurrencies of the Neighbor
  *        item with the reference item
  */
case class Neighbor( item: Int, similarity: Double, coOccurrencies: Int )

object Neighbor {
  /** Default ordering of neighbors by descending similarity */
  implicit val defaultOrdering = Ordering.fromLessThan[Neighbor] { ( a, b ) =>
    if ( a.similarity == b.similarity )
      a.item > b.item
    else
      a.similarity > b.similarity
  }
}
