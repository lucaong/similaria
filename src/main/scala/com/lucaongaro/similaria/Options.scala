package com.lucaongaro.similaria

case class Options(
  dbPath: String,
  dbSize: Long
)

object Options {
  implicit val default = new Options(
    "db/similaria",
    1000 * 1048576  // 1GB
  )
}
