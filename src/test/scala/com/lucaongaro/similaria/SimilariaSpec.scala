import com.lucaongaro.similaria.{ Similaria, Options }
import com.lucaongaro.similaria.lmdb.DBManager
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import scala.language.postfixOps
import java.io.File
import org.apache.commons.io.FileUtils

class SimilariaSpec extends FunSpec with ShouldMatchers {
  private var rec: Similaria = _
  private var dbPath = "./tmp/testdb"
  private val file   = new File( dbPath )

  override def withFixture( test: NoArgTest ) {
    // Prepare and cleanup test db directory
    file.mkdirs()
    FileUtils.cleanDirectory( file )

    implicit val opts = new Options( dbPath, 10485760 )
    rec = new Similaria()
    try
      test()
    finally {
      rec.dbm.close()
      FileUtils.cleanDirectory( file )
    }
  }

  describe("Similaria") {
    describe("addPreferenceSet") {
      it("increments occurrencies and co-occurencies for set") {
        rec.addPreferenceSet( List(123L, 456L).toSet )
        rec.dbm.getOccurrency( 123 ) should be(1)
        rec.dbm.getOccurrency( 456 ) should be(1)
        rec.dbm.getCoOccurrencies( 123 ) should be(
          List((456L, 1, 1))
        )
      }

      it("returns the added set") {
        val set = rec.addPreferenceSet( List(123L, 456L).toSet )
        set should be( List(123L, 456L).toSet )
      }
    }

    describe("addToPreferenceSet") {
      it("appends to a pre-existing preference set") {
        rec.addPreferenceSet( List(1L, 2L).toSet )
        rec.addToPreferenceSet( List(1L, 2L).toSet, List(3L, 4L).toSet )
        rec.dbm.getOccurrency( 1 ) should be(1)
        rec.dbm.getOccurrency( 2 ) should be(1)
        rec.dbm.getOccurrency( 3 ) should be(1)
        rec.dbm.getOccurrency( 4 ) should be(1)
        val occurrencies = rec.dbm.getCoOccurrencies( 1 ).sortBy( _._1 )
        occurrencies should be(
          List((2, 1, 1), (3, 1, 1), (4, 1, 1))
        )
      }

      it("returns the resulting set") {
        val set = rec.addToPreferenceSet( List(1L, 2L).toSet, List(3L, 4L).toSet )
        set should be( List(1L, 2L, 3L, 4L).toSet )
      }
    }

    describe("removePreferenceSet") {
      it("decrements occurrencies and co-occurencies for set") {
        rec.addPreferenceSet( List(123L, 456L).toSet )
        rec.removePreferenceSet( List(123L, 456L).toSet )
        rec.dbm.getOccurrency( 123 ) should be( 0 )
        rec.dbm.getOccurrency( 456 ) should be( 0 )
        rec.dbm.getCoOccurrencies( 123 ) should be( Nil )
      }

      it("returns the removed set") {
        val set = rec.removePreferenceSet( List(123L, 456L).toSet )
        set should be( List(123L, 456L).toSet )
      }
    }

    describe("removeFromPreferenceSet") {
      it("remove items from a pre-existing preference set") {
        rec.addPreferenceSet( List(1L, 2L, 3L, 4L).toSet )
        rec.removeFromPreferenceSet( List(1L, 2L, 3L, 4L).toSet, List(3L, 4L).toSet )
        rec.dbm.getOccurrency( 1 ) should be( 1 )
        rec.dbm.getOccurrency( 2 ) should be( 1 )
        rec.dbm.getOccurrency( 3 ) should be( 0 )
        rec.dbm.getOccurrency( 4 ) should be( 0 )
        val occurrencies = rec.dbm.getCoOccurrencies( 1 ).sortBy( _._1 )
        occurrencies should be(
          List( (2, 1, 1) )
        )
      }

      it("returns the resulting set") {
        val set = rec.removeFromPreferenceSet( List(1L, 2L, 3L, 4L).toSet, List(3L, 4L).toSet )
        set should be( List(1L, 2L).toSet )
      }
    }

    describe("findNeighborsOf") {
      it("gives recommendations on the basis of submitted preference sets") {
        rec.addPreferenceSet( List(123L, 456L, 789L).toSet )
        rec.addPreferenceSet( List(1L, 2L, 3L).toSet )
        rec.addPreferenceSet( List(123L, 456L).toSet )
        val neighbors = rec.findNeighborsOf( 123 )
        neighbors.toList match {
          case first :: second :: Nil =>
            first.item should be( 456 )
            first.similarity should be( 1.0 )
            second.item should be( 789 )
            second.similarity should be( 0.5 )
          case _ => throw new Exception("unexpected result")
        }
      }

      it("returns empty recomendation list if there is no such item") {
        rec.addPreferenceSet( List(123L, 456L, 789L).toSet )
        rec.addPreferenceSet( List(1L, 2L, 3L).toSet )
        rec.addPreferenceSet( List(123L, 456L).toSet )
        val neighbors = rec.findNeighborsOf( 321 )
        neighbors.toList should be( Nil )
      }
    }

    describe("getSimilarityBetween") {
      it("returns 0.0 if the items never occurred together") {
        rec.getSimilarityBetween( 123, 321 ) should be( 0.0 )
      }

      it("returns the similarity between two items") {
        rec.addPreferenceSet( List(1L, 2L, 3L).toSet )
        rec.addPreferenceSet( List(1L, 2L).toSet )
        rec.getSimilarityBetween( 1, 2 ) should be( 1.0 )
        rec.getSimilarityBetween( 1, 3 ) should be( 0.5 )
      }
    }
  }
}
