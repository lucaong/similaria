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
limit - although very high - on how much load it can handle. On the other hand
is very easy to setup, and it can be trained online, with no need for batch
background processing. You just start it and it's ready to learn from new data
and give recommendations.


## How Performant Is It?

I went through several iterations to optimize Similaria, in the effort of making
it as fast as possible, effectively pushing its upper limit. My intention is to
publish a more complete and meaningful benchmark and performance analysis, but
for now these are my results testing it locally on a 2009 MacBook Pro with SSD.
I wrapped Similaria in a thin HTTP API to serve recommendations in JSON format.
I trained the engine with the LastFM 360K dataset. When asking for 10
recommended artists for people who like "The Beatles", I could reach ~3000
requests per second:

    Concurrency Level:      4
    Time taken for tests:   0.628 seconds
    Complete requests:      2000
    Failed requests:        0
    Write errors:           0
    Total transferred:      1296000 bytes
    HTML transferred:       1058000 bytes
    Requests per second:    3186.00 [#/sec] (mean)
    Time per request:       1.255 [ms] (mean)
    Time per request:       0.314 [ms] (mean, across all concurrent requests)
    Transfer rate:          2016.14 [Kbytes/sec] received

    Connection Times (ms)
                  min  mean[+/-sd] median   max
    Connect:        0    0   0.2      0       4
    Processing:     0    1   0.5      1       5
    Waiting:        0    1   0.5      1       5
    Total:          0    1   0.5      1       6

    Percentage of the requests served within a certain time (ms)
      50%      1
      66%      1
      75%      1
      80%      1
      90%      2
      95%      2
      98%      3
      99%      3
     100%      6 (longest request)


## Usage

```scala
import com.lucaongaro.similaria._

val opts = Options(
    dbPath: "db/similaria", // Directory where to persist data (must exist)
    dbSize: 1073741824      // Maximum data size (here 1GB). Can be increased later.
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

/* Tell similaria to learn this preference set: */
for ( set <- preferenceSets ) similaria.addPreferenceSet( set )

/*
*  If at a later date user 1 also likes items 34 and 52, you can tell
*  similaria to append them to the previous preference set (provided that you
*  still know what the original preference set was):
*/
similaria.addToPreferenceSet( preferenceSets.head, Set( 34, 52 ) )

/*
*  Similaria also provides methods to forget a preference set or part of it.
*  Note how similaria does not know directly about users, but only about
*  preference sets.
*/

/* ---- Getting Recommendations: ----
*
*  You can give an item ID to similaria, and ask for a number of recommended
*  items. These are the items that tend to co-occur in the same preference sets
*  as the given item. We call these items the "neighbors" of the reference
*  item. The recommended items are instances of the `Neighbor` case class, and
*  ordered by a similarity measure (Jaccard distance) that goes from 0 (no
*  similarity) to 1 (perfect similarity).
*
*  For example, if we want to get 10 recommendations for item with ID 5:
*/
val neighbors = similaria.findNeighborsOf( 5, limit = 10 )

println("If you liked item 5 you might also like:")
neighbors.foreach { n =>
  println s"Item ${n.item} (similarity: ${n.similarity}, co-occurrencies: ${n.coOccurrencies})"
}
```

## Credit

Similaria persists data using the Symas Lightning Memory-Mapped Database
(LMDB), which is licensed under the OpenLDAP Public License. The Java binding
is provided by lmdbjni, which is licensed under the Apache License, Version
2.0.
