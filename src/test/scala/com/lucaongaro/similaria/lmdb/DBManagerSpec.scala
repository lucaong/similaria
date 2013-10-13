import com.lucaongaro.similaria.lmdb.{ DBManager, DBOptions }
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import scala.language.postfixOps
import java.io.File
import org.apache.commons.io.FileUtils

class DBManagerSpec extends FunSpec with ShouldMatchers {

  private var dbm: DBManager = _
  private val file = new File("./tmp/testdb")

  override def withFixture( test: NoArgTest ) {
    val opts = new DBOptions( "./tmp/testdb", 10485760, 0.3 )
    dbm = new DBManager( opts )
    try
      test()
    finally {
      dbm.close()
      FileUtils.cleanDirectory( file )
    }
  }

  describe("DBManager") {
    describe("getOccurrency") {
      it("returns 0 if the key was never set") {
        dbm.getOccurrency( 123 ) should be( 0 )
      }
    }

    describe("incrementOccurrency") {
      it("increments the occurrency count for an item") {
        dbm.getOccurrency( 123 ) should be( 0 )
        dbm.incrementOccurrency( 123, 42 )
        dbm.getOccurrency( 123 ) should be( 42 )
        dbm.incrementOccurrency( 123, 4 )
        dbm.getOccurrency( 123 ) should be( 46 )
      }
    }

    describe("getCoOccurrencies") {
      it("returns empty list if there are none") {
        val coOcc = dbm.getCoOccurrencies( 123, 10 )
        coOcc should be( Nil )
      }

      it("returns a list of (key, score) tuples ordered by descending score") {
        dbm.incrementCoOccurrency( 123, 213, 1 )
        dbm.incrementCoOccurrency( 123, 132, 2 )
        dbm.incrementCoOccurrency( 123, 312, 3 )
        val coOcc = dbm.getCoOccurrencies( 123, 10 )
        coOcc match {
          case one::two::three::Nil =>
            one._1 should be( 312 )
            one._2 should be( 3 )
            two._1 should be( 132 )
            two._2 should be( 2 )
            three._1 should be( 213 )
            three._2 should be( 1 )
          case _ => println(coOcc); throw new Exception("unexpected result")
        }
      }

      it("only gets co-occurrencies of the given item") {
        dbm.incrementCoOccurrency( 123, 456, 1 )
        dbm.incrementCoOccurrency( 123, 567, 2 )
        dbm.incrementCoOccurrency( 321, 789, 3 )
        val coOcc = dbm.getCoOccurrencies( 123, 10 )
        coOcc match {
          case one::two::Nil =>
            one._1 should be( 567 )
            one._2 should be( 2 )
            two._1 should be( 456 )
            two._2 should be( 1 )
          case _ => println(coOcc); throw new Exception("unexpected result")
        }
      }
    }
  }
}
