package com.lucaongaro.similaria

case class Options(
  dbPath:     String,
  dbSize:     Long,
  similarity: ( Long, Long, Long ) => Double
)

object Options {
  implicit val default = new Options(
    "db/similaria",
    1000 * 1048576,
    ( aCount, bCount, abCount ) =>
      abCount.toDouble / ( aCount + bCount - abCount )
  )
}
