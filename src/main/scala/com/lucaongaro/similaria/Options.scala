package com.lucaongaro.similaria

/** Configuration options for [[com.lucaongaro.similaria.Similaria]]
  * instances
  *
  * @constructor creates an Option object
  * @param dbPath the path where to persist the database
  * @param dbSize the maximum size of the database in bytes
  */
case class Options(
  dbPath: String,
  dbSize: Long
)

object Options {
  /** Implicit default value for the options
    *
    * By default uses a path of 'db/similaria' and a size of 1GB
    */
  implicit val default = new Options(
    "db/similaria",
    1000 * 1048576  // 1GB
  )
}
