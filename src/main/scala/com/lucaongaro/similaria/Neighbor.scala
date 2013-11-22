package com.lucaongaro.similaria

case class Neighbor( item: Int, similarity: Double )

object Neighbor {
  // Default ordering is by descending similarity
  implicit val defaultOrdering = Ordering.fromLessThan[Neighbor] { ( a, b ) =>
    if ( a.similarity == b.similarity )
      a.item > b.item
    else
      a.similarity > b.similarity
  }
}
