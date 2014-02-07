# Similaria

Similaria is a Scala library implementing a recommendation engine, namely
item-item binary collaborative filtering. In practice, it can infer knowledge
like:

  - People that like this item also like...
  - Users that bought this item also buy...
  - If you loved this item you may also love these other ones...

Similaria is particularly well suited for e-commerce and other similar
contexts: when a user express interests for a product (e.g. she
likes/visits/pins a product) other related products can be recommended.


## A Convenient Engine for "Not-Too-Big Data"

Similaria is a self-contained, centralized engine. As such, there is an upper
limit - although very high - on how much traffic it can handle. On the other
hand is very easy to setup, and it can be trained online, with no need for
batch background processing. You just start it and it's ready to learn from new
data and give recommendations.


## Usage

```scala
import com.lucaongaro.similaria._

val opts = Options(
    dbPath: "db/similaria", // Directory where to persist data (must exist)
    dbSize: 1048576000      // Maximum size of dataset (here 1GB). Can be increased later.
  )

val similaria = new Similaria( opts )

/*
*  ---- Training Similaria: ----
*
*  Say that some users liked the following items (referenced by numeric IDs):
* 
*  | User | Items liked  |
*  |  1   | 1, 14, 5, 45 |
*  |  2   | 34, 3        |
*  |  3   | 2, 15, 5, 1  |
*  |  4   | 5, 1, 2      |
*
*  We call a set of items liked by the same user a "preference set". Similaria
*  learns by reflecting on preference sets:
*/

val preferenceSets = List(
  Set( 1, 14, 5, 45 ),
  Set( 34, 3 ),
  Set( 2, 15, 5, 1 ),
  Set( 5, 1, 2 )
)

// Tell similaria to learn this preference set
for ( set <- preferenceSets ) similaria.addPreferenceSet( set )

/*
*  If at a later date user 1 also likes items 34 and 52, you can tell
*  similaria to append them to the previous preference set (provided that you
*  still know what the original preference set was):
*/
similaria.addToPreferenceSet( preferenceSets.head, Set( 34, 52 ) )

/* ---- Getting Recommendations: ----
*
*  You can give an item ID to similaria, and ask for a number of recommended
*  items. These are the items that tend to co-occur in the same preference sets
*  as the given item. We call them the most similar items, or "neighbors". The
*  recommended items are ordered by a similarity measure (Jaccard distance)
*  that goes from 0 (no similarity) to 1 (perfect similarity).
*
*  For example, if we want to get 10 recommendations for item with ID 5:
*/
val neighbors = similaria.findNeighborsOf( 5, limit = 10 )

println "If you liked item 5 you might also like:"
neighbors.foreach { n =>
  println s" - Item ${n.item} (similarity: ${n.similarity}, co-occurrencies: ${n.coOccurrencies})"
}
```
