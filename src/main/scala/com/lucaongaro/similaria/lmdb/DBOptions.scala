package com.lucaongaro.similaria.lmdb


case class DBOptions(
  path:  String,
  size:  Long,
  alpha: Double
)

object DBOptions {
  implicit val default = new DBOptions(
    "db/similaria",
    1000 * 1048576,
    0.3
  )
}
