import com.lucaongaro.similaria.Similaria
import com.lucaongaro.similaria.lmdb.{ DBManager, DBOptions }
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import scala.language.postfixOps
import java.io.File
import org.apache.commons.io.FileUtils

class SimilariaSpec extends FunSpec with ShouldMatchers {
  private var rec: Similaria = _
  private val file = new File("./tmp/testdb")

  override def withFixture( test: NoArgTest ) {
    implicit val opts = new DBOptions( "./tmp/testdb", 10485760, 0.3 )
    rec = new Similaria()
    try
      test()
    finally {
      rec.dbm.close()
      FileUtils.cleanDirectory( file )
    }
  }

  describe("Recommender") {
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
