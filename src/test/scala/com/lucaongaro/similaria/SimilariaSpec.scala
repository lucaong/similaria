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
        rec.addPreferenceSet( List(123, 456).toSet )
        rec.dbm.getOccurrency( 123 ) should be(1)
        rec.dbm.getOccurrency( 456 ) should be(1)
        rec.dbm.withCoOccurrencyIterator( 123 )( _.toList ) should be(
          List((456, 1, 1))
        )
      }

      it("accepts a count to add the set more than once") {
        rec.addPreferenceSet( List(123, 456).toSet, 3 )
        rec.dbm.getOccurrency( 123 ) should be(3)
        rec.dbm.getOccurrency( 456 ) should be(3)
        rec.dbm.withCoOccurrencyIterator( 123 )( _.toList ) should be(
          List((456, 3, 3))
        )
      }

      it("returns the added set") {
        val set = rec.addPreferenceSet( List(123, 456).toSet )
        set should be( List(123, 456).toSet )
      }
    }

    describe("addToPreferenceSet") {
      it("appends to a pre-existing preference set") {
        rec.addPreferenceSet( List(1, 2).toSet )
        rec.addToPreferenceSet( List(1, 2).toSet, List(3, 4).toSet )
        rec.dbm.getOccurrency( 1 ) should be(1)
        rec.dbm.getOccurrency( 2 ) should be(1)
        rec.dbm.getOccurrency( 3 ) should be(1)
        rec.dbm.getOccurrency( 4 ) should be(1)
        val occurrencies = rec.dbm.withCoOccurrencyIterator( 1 )( _.toList ).sortBy( _._1 )
        occurrencies should be(
          List((2, 1, 1), (3, 1, 1), (4, 1, 1))
        )
      }

      it("accepts a count to append the subset more than once") {
        rec.addPreferenceSet( List(1, 2).toSet, 3 )
        rec.addToPreferenceSet( List(1, 2).toSet, List(3, 4).toSet, 3 )
        rec.dbm.getOccurrency( 1 ) should be(3)
        rec.dbm.getOccurrency( 2 ) should be(3)
        rec.dbm.getOccurrency( 3 ) should be(3)
        rec.dbm.getOccurrency( 4 ) should be(3)
        val occurrencies = rec.dbm.withCoOccurrencyIterator( 1 )( _.toList ).sortBy( _._1 )
        occurrencies should be(
          List((2, 3, 3), (3, 3, 3), (4, 3, 3))
        )
      }

      it("returns the resulting set") {
        val set = rec.addToPreferenceSet( List(1, 2).toSet, List(3, 4).toSet )
        set should be( List(1, 2, 3, 4).toSet )
      }
    }

    describe("removePreferenceSet") {
      it("decrements occurrencies and co-occurencies for set") {
        rec.addPreferenceSet( List(123, 456).toSet )
        rec.removePreferenceSet( List(123, 456).toSet )
        rec.dbm.getOccurrency( 123 ) should be( 0 )
        rec.dbm.getOccurrency( 456 ) should be( 0 )
        rec.dbm.withCoOccurrencyIterator( 123 ) { _.isEmpty should be( true ) }
      }

      it("accepts a count to remove the set more than once") {
        rec.addPreferenceSet( List(123, 456).toSet, 3 )
        rec.removePreferenceSet( List(123, 456).toSet, 3 )
        rec.dbm.getOccurrency( 123 ) should be( 0 )
        rec.dbm.getOccurrency( 456 ) should be( 0 )
        rec.dbm.withCoOccurrencyIterator( 123 ) { _.isEmpty should be( true ) }
      }

      it("returns the removed set") {
        val set = rec.removePreferenceSet( List(123, 456).toSet )
        set should be( List(123, 456).toSet )
      }
    }

    describe("removeFromPreferenceSet") {
      it("remove items from a pre-existing preference set") {
        rec.addPreferenceSet( List(1, 2, 3, 4).toSet )
        rec.removeFromPreferenceSet( List(1, 2, 3, 4).toSet, List(3, 4).toSet )
        rec.dbm.getOccurrency( 1 ) should be( 1 )
        rec.dbm.getOccurrency( 2 ) should be( 1 )
        rec.dbm.getOccurrency( 3 ) should be( 0 )
        rec.dbm.getOccurrency( 4 ) should be( 0 )
        val occurrencies = rec.dbm.withCoOccurrencyIterator( 1 )( _.toList ).sortBy( _._1 )
        occurrencies should be(
          List( (2, 1, 1) )
        )
      }

      it("accepts a count to remove the subset more than once") {
        rec.addPreferenceSet( List(1, 2, 3, 4).toSet, 3 )
        rec.removeFromPreferenceSet( List(1, 2, 3, 4).toSet, List(3, 4).toSet, 3 )
        rec.dbm.getOccurrency( 1 ) should be( 3 )
        rec.dbm.getOccurrency( 2 ) should be( 3 )
        rec.dbm.getOccurrency( 3 ) should be( 0 )
        rec.dbm.getOccurrency( 4 ) should be( 0 )
        val occurrencies = rec.dbm.withCoOccurrencyIterator( 1 )( _.toList ).sortBy( _._1 )
        occurrencies should be(
          List( (2, 3, 3) )
        )
      }

      it("returns the resulting set") {
        val set = rec.removeFromPreferenceSet( List(1, 2, 3, 4).toSet, List(3, 4).toSet )
        set should be( List(1, 2).toSet )
      }
    }

    describe("findNeighborsOf") {
      it("gives recommendations on the basis of submitted preference sets") {
        rec.addPreferenceSet( List(123, 456, 789).toSet )
        rec.addPreferenceSet( List(1, 2, 3).toSet )
        rec.addPreferenceSet( List(123, 456).toSet )
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

      it("returns empty recommendation list if there is no such item") {
        rec.addPreferenceSet( List(123, 456, 789).toSet )
        rec.addPreferenceSet( List(1, 2, 3).toSet )
        rec.addPreferenceSet( List(123, 456).toSet )
        val neighbors = rec.findNeighborsOf( 321 )
        neighbors.toList should be( Nil )
      }
    }

    describe("getSimilarityBetween") {
      it("returns 0.0 if the items never occurred together") {
        rec.getSimilarityBetween( 123, 321 ) should be( 0.0 )
      }

      it("returns the similarity between two items") {
        rec.addPreferenceSet( List(1, 2, 3).toSet )
        rec.addPreferenceSet( List(1, 2).toSet )
        rec.getSimilarityBetween( 1, 2 ) should be( 1.0 )
        rec.getSimilarityBetween( 1, 3 ) should be( 0.5 )
      }
    }

    describe("muteItem") {
      it("prevents the item to appear in recommendation") {
        rec.addPreferenceSet( List(1, 2, 3, 4).toSet )
        rec.addPreferenceSet( List(1, 2, 3).toSet )
        rec.muteItem( 2 )
        val neighbors = rec.findNeighborsOf( 1 )
        neighbors.toList match {
          case first :: second :: Nil =>
            first.item  should be( 3 )
            second.item should be( 4 )
          case _ => throw new Exception("unexpected result")
        }
      }
    }

    describe("unmuteItem") {
      it("allow the item to appear in recommendation") {
        rec.addPreferenceSet( List(1, 2, 3, 4).toSet )
        rec.addPreferenceSet( List(1, 2, 3).toSet )
        rec.addPreferenceSet( List(1, 2).toSet )
        rec.muteItem( 2 )
        rec.unmuteItem( 2 )
        val neighbors = rec.findNeighborsOf( 1 )
        neighbors.toList match {
          case first :: second :: third :: Nil =>
            first.item  should be( 2 )
            second.item should be( 3 )
            third.item  should be( 4 )
          case _ => throw new Exception("unexpected result")
        }
      }
    }
  }
}
