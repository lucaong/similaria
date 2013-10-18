import com.lucaongaro.similaria.lmdb.DBManager
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import scala.language.postfixOps
import java.io.File
import org.apache.commons.io.FileUtils

class DBManagerSpec extends FunSpec with ShouldMatchers {

  private var dbm: DBManager = _
  private var dbPath = "./tmp/testdb"
  private val file   = new File( dbPath )
  file.mkdirs()

  override def withFixture( test: NoArgTest ) {
    // Prepare and cleanup test db directory
    FileUtils.cleanDirectory( file )
    dbm = new DBManager( dbPath, 10485760 )
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

      it("decrements the occurrency count if given a negative increment") {
        dbm.incrementOccurrency( 123, 46 )
        dbm.incrementOccurrency( 123, -4 )
        dbm.getOccurrency( 123 ) should be( 42 )
      }

      it("never decrements a count below 0") {
        dbm.incrementOccurrency( 123, 42 )
        dbm.incrementOccurrency( 123, -46 )
        dbm.getOccurrency( 123 ) should be( 0 )
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

    describe("incrementCoOccurrency") {
      it("increments co-occurrency for a pair of items") {
        dbm.incrementCoOccurrency( 123, 456, 3 )
        dbm.getCoOccurrencies( 123, 10 ) should be( List( (456L, 3) ) )
        dbm.getCoOccurrencies( 456, 10 ) should be( List( (123L, 3) ) )
      }

      it("decrements co-occurrency if given a negative increment") {
        dbm.incrementCoOccurrency( 123, 456, 3 )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123, 10 ) should be( List( (456L, 1) ) )
        dbm.getCoOccurrencies( 456, 10 ) should be( List( (123L, 1) ) )
      }

      it("deletes co-occurrency if decremented to or below 0") {
        dbm.incrementCoOccurrency( 123, 456, 2 )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123, 10 ) should be( Nil )
        dbm.getCoOccurrencies( 456, 10 ) should be( Nil )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123, 10 ) should be( Nil )
        dbm.getCoOccurrencies( 456, 10 ) should be( Nil )
      }
    }

    describe("copy") {
      it("creates a backup of the data at the specified location") {
        val copyLocation = "./tmp/testdbcopy"
        val copyFile     = new File( copyLocation )

        // Prepare and cleanup db backup directory
        copyFile.mkdirs()
        FileUtils.cleanDirectory( copyFile )

        dbm.incrementOccurrency( 123, 3 )
        dbm.incrementCoOccurrency( 123, 456, 2 )
        dbm.incrementCoOccurrency( 123, 789, 1 )
        dbm.copy( copyLocation )

        val copy = new DBManager( copyLocation, 10485760 )
        try {
          copy.getOccurrency( 123 ) should be( 3 )
          copy.getCoOccurrencies( 123, 3 ) should be(
            List( (456L, 2), (789L, 1) )
          )
        } finally {
          copy.close()
          FileUtils.cleanDirectory( copyFile )
        }
      }
    }
  }
}
