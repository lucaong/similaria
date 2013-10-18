import com.lucaongaro.similaria.{ Similaria, Options }
import com.lucaongaro.similaria.lmdb.DBManager
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import scala.language.postfixOps
import java.io.File
import org.apache.commons.io.FileUtils

class SimilariaSpec extends FunSpec with ShouldMatchers {
  private var rec: Similaria = _
  private val file = new File("./tmp/testdb")

  override def withFixture( test: NoArgTest ) {
    val similarity = ( a: Long, b: Long, ab: Long ) =>
      ab.toDouble / ( a + b - ab )
    implicit val opts = new Options( "./tmp/testdb", 10485760, similarity )
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
        rec.dbm.getCoOccurrencies( 123, 2 ) should be(
          List((456L, 1))
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
        val occurrencies = rec.dbm.getCoOccurrencies( 1, 3 ).sortBy( _._1 )
        occurrencies should be(
          List((2L, 1), (3L, 1), (4L, 1))
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
        rec.dbm.getOccurrency( 123 ) should be(0)
        rec.dbm.getOccurrency( 456 ) should be(0)
        rec.dbm.getCoOccurrencies( 123, 2 ) should be(
          List((456L, 0))
        )
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
        rec.dbm.getOccurrency( 1 ) should be(1)
        rec.dbm.getOccurrency( 2 ) should be(1)
        rec.dbm.getOccurrency( 3 ) should be(0)
        rec.dbm.getOccurrency( 4 ) should be(0)
        val occurrencies = rec.dbm.getCoOccurrencies( 1, 3 ).sortBy( _._1 )
        occurrencies should be(
          List((2L, 1), (3L, 0), (4L, 0))
        )
      }

      it("returns the resulting set") {
        val set = rec.removeFromPreferenceSet( List(1L, 2L, 3L, 4L).toSet, List(3L, 4L).toSet )
        set should be( List(1L, 2L).toSet )
      }
    }

    describe("getNeighborsOf") {
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
  }
}
