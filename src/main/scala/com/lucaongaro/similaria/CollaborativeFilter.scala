package com.lucaongaro.similaria

trait CollaborativeFilter {
  import scala.collection.SortedSet

  def addPreferenceSet(
    prefSet: Set[Long]
  ): Set[Long]

  def addToPreferenceSet(
    originalSet: Set[Long],
    setToAdd:    Set[Long]
  ): Set[Long]

  def removePreferenceSet(
    prefSet: Set[Long]
  ): Set[Long]

  def removeFromPreferenceSet(
    originalSet: Set[Long],
    setToRemove: Set[Long]
  ): Set[Long]

  def findNeighborsOf(
    item:  Long,
    limit: Integer
  ): SortedSet[Neighbor]
}
