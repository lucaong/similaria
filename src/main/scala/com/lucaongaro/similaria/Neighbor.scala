package com.lucaongaro.similaria

/** The representation of an item as a neighbor of another reference item
  *
  * @constructor creates a new neighbor
  * @param item the neighbor item
  * @param similarity the similarity with respect to the reference item
  * @param count the occurrency count of the neighbor item
  */
case class Neighbor( item: Int, similarity: Double, count: Int )

object Neighbor {
  /** Default ordering of neighbors by descending similarity */
  implicit val defaultOrdering = Ordering.fromLessThan[Neighbor] { ( a, b ) =>
    if ( a.similarity == b.similarity )
      a.item > b.item
    else
      a.similarity > b.similarity
  }
}
