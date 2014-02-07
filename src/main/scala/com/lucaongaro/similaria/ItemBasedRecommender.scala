package com.lucaongaro.similaria

import scala.collection.SortedSet

/** A trait defining the interface for the item-based recommendation engine. */
trait ItemBasedRecommender {
  type PreferenceSet = Set[Int]

  /** Adds a preference set
    *
    * @param prefSet the preference set to be added
    * @param count how many times the preference set should be added
    * @return the set that was added
    */
  def addPreferenceSet(
    prefSet: PreferenceSet,
    count:   Int = 1
  ): PreferenceSet

  /** Appends a subset to an already existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToAdd the subset to be added
    * @param count how many times the subset should be added
    * @return the resulting set
    */
  def addToPreferenceSet(
    originalSet: PreferenceSet,
    setToAdd:    PreferenceSet,
    count:       Int = 1
  ): PreferenceSet

  /** Removes a preference set
    *
    * @param prefSet the preference set to be removed
    * @return the set that was removed
    * @param count how many times the preference set should be removed
    */
  def removePreferenceSet(
    prefSet: PreferenceSet,
    count:   Int = 1
  ): PreferenceSet

  /** Removes a subset from an existing preference set
    *
    * @param originalSet the pre-existing set
    * @param setToRemove the subset to be removed
    * @param count how many times the subset should be removed
    * @return the resulting set
    */
  def removeFromPreferenceSet(
    originalSet: PreferenceSet,
    setToRemove: PreferenceSet,
    count:       Int = 1
  ): PreferenceSet

  /** Returns the nearest neighbors of the given item
    *
    * @param item the reference item
    * @param limit the maximum number of neighbors to retrieve
    * @return a sorted set of the nearest neighbors
    */
  def findNeighborsOf(
    item:  Int,
    limit: Int = 10
  ): SortedSet[Neighbor]

  /** Returns the similarity between two items
    *
    * @param item the first item
    * @param other the other item
    * @return the similarity between item and other
    */
  def getSimilarityBetween(
    item:  Int,
    other: Int
  ): Double

  /** Mutes an item (so that it is not considered when getting neighbors)
    *
    * @param item the item to be muted
    */
  def muteItem(
    item: Int
  )

  /** Unmutes an item (so that it is considered when getting neighbors)
    *
    * @param item the item to be unmuted
    */
  def unmuteItem(
    item: Int
  )
}
