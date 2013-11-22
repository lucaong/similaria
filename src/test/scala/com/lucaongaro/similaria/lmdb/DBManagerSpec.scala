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

    describe("getCoOccurrency") {
      it("returns 0 if there is no co-occurrency") {
        dbm.getCoOccurrency( 1, 2 ) should be( 0 )
      }

      it("returns the co-occurrency") {
        dbm.incrementCoOccurrency( 1, 2, 3 )
        dbm.getCoOccurrency( 1, 2 ) should be( 3 )
      }
    }

    describe("getCoOccurrencies") {
      it("returns empty list if there are none") {
        val coOcc = dbm.getCoOccurrencies( 123 )
        coOcc should be( Nil )
      }

      it("returns a list of (key, co-count, count) tuples ordered by descending score") {
        dbm.incrementOccurrency( 213, 3 )
        dbm.incrementOccurrency( 132, 2 )
        dbm.incrementOccurrency( 312, 1 )

        dbm.incrementCoOccurrency( 123, 213, 1 )
        dbm.incrementCoOccurrency( 123, 132, 2 )
        dbm.incrementCoOccurrency( 123, 312, 3 )

        val coOcc = dbm.getCoOccurrencies( 123 )
        coOcc match {
          case one::two::three::Nil =>
            one   should be( (312, 3, 1) )
            two   should be( (132, 2, 2) )
            three should be( (213, 1, 3) )
          case _ => throw new Exception("unexpected result")
        }
      }

      it("only gets co-occurrencies of the given item") {
        dbm.incrementCoOccurrency( 123, 456, 1 )
        dbm.incrementCoOccurrency( 123, 567, 2 )
        dbm.incrementCoOccurrency( 321, 789, 3 )

        val coOcc = dbm.getCoOccurrencies( 123 )
        coOcc match {
          case one::two::Nil =>
            one should be( (567, 2, 0) )
            two should be( (456, 1, 0) )
          case _ => throw new Exception("unexpected result")
        }
      }

      it("gets only the first n co-occurrencies if given a limit") {
        dbm.incrementCoOccurrency( 123, 456, 1 )
        dbm.incrementCoOccurrency( 123, 567, 2 )
        dbm.incrementCoOccurrency( 123, 789, 3 )

        val coOcc = dbm.getCoOccurrencies( 123, 2 )
        coOcc.length should be( 2 )
        coOcc match {
          case one::two::Nil =>
            one should be( (789, 3, 0) )
            two should be( (567, 2, 0) )
          case _ => throw new Exception("unexpected result")
        }
      }

      it("only considers non-muted items") {
        dbm.incrementCoOccurrency( 1, 2, 1 )
        dbm.incrementCoOccurrency( 1, 3, 2 )
        dbm.incrementCoOccurrency( 1, 4, 3 )
        dbm.incrementCoOccurrency( 2, 5, 3 )
        dbm.incrementOccurrency( 3, 1 )
        dbm.setMuted( 3, true )

        val coOcc = dbm.getCoOccurrencies( 1, 2 )
        coOcc.length should be( 2 )
        coOcc match {
          case one::two::Nil =>
            one should be( (4, 3, 0) )
            two should be( (2, 1, 0) )
          case _ => throw new Exception("unexpected result")
        }
      }
    }

    describe("incrementCoOccurrency") {
      it("increments co-occurrency for a pair of items") {
        dbm.incrementCoOccurrency( 123, 456, 3 )
        dbm.getCoOccurrencies( 123 ) should be( List( (456, 3, 0) ) )
        dbm.getCoOccurrencies( 456 ) should be( List( (123, 3, 0) ) )
      }

      it("decrements co-occurrency if given a negative increment") {
        dbm.incrementCoOccurrency( 123, 456, 3 )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123 ) should be( List( (456, 1, 0) ) )
        dbm.getCoOccurrencies( 456 ) should be( List( (123, 1, 0) ) )
      }

      it("deletes co-occurrency if decremented to or below 0") {
        dbm.incrementCoOccurrency( 123, 456, 2 )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123 ) should be( Nil )
        dbm.getCoOccurrencies( 456 ) should be( Nil )
        dbm.incrementCoOccurrency( 123, 456, -2 )
        dbm.getCoOccurrencies( 123 ) should be( Nil )
        dbm.getCoOccurrencies( 456 ) should be( Nil )
      }
    }

    describe("setMuted") {
      it("sets the muted state of the given item") {
        dbm.incrementOccurrency( 123, 3 )
        dbm.setMuted( 123, true )
        dbm.getOccurrencyUnlessMuted( 123 ) should be( None )
        dbm.setMuted( 123, false )
        dbm.getOccurrencyUnlessMuted( 123 ) should be( Some(3) )
      }

      it("has no effect if the item had no occurrency") {
        dbm.setMuted( 123, true )
        dbm.getOccurrencyUnlessMuted( 123 ) should be( Some(0) )
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
        dbm.incrementOccurrency( 456, 5 )
        dbm.incrementCoOccurrency( 123, 456, 2 )
        dbm.incrementCoOccurrency( 123, 789, 1 )
        dbm.copy( copyLocation )

        val copy = new DBManager( copyLocation, 10485760 )
        try {
          copy.getOccurrency( 123 ) should be( 3 )
          copy.getCoOccurrencies( 123 ) should be(
            List( (456, 2, 5), (789, 1, 0) )
          )
        } finally {
          copy.close()
          FileUtils.cleanDirectory( copyFile )
        }
      }
    }
  }
}
