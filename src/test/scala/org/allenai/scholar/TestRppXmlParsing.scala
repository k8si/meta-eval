package org.allenai.scholar

import java.time.Year

import org.allenai.common.Resource
import org.allenai.common.testkit.UnitSpec
import org.allenai.scholar.metrics.metadata.{ PaperMetadata, RPPParser }
import org.jsoup.Jsoup

import scala.io.Source

import scala.collection.mutable

class TestRppXmlParsing extends UnitSpec {
  val MetadataAndBibliography(metadata, bibs) =
    RPPParser.extractMetadataAndBib(IoHelpers.loadXmlFile("rpp_test.xml"))

  "RPP output" should "parse metadata correctly" in {
    val expectedTitle = Title("ALGORITHMS THAT LEARN TO EXTRACT INFORMATION m  BBN: TIPSTER PHASE III")
    metadata.title should equal(expectedTitle)
    //    metadata.venue should equal(Venue(""))
    //    metadata.year should equal(Year.of(1995))
    val expectedAuthors = Set(
      Author("Michael", List(), "Crystal"),
      Author("Scott", List(), "Miller")
    )
    println("GOT AUTHORS:")
    metadata.authors.toSet.foreach(println)
    metadata.authors.toSet should equal(expectedAuthors)
  }

  "Bibliography" should "be extracted correctly" in {
    val doc = RPPParser.extractStructuredDocument(IoHelpers.loadXmlFile("rpp_test.xml"))
    val bib = doc.bibliography
    bib.entries.size should equal(3)
    val expectedAuthors = List(
      Set(Author("D", List(), "Harman")),
      Set(Author("D", List(), "Harman")),
      Set(Author("Jones", List(), "Sparck"), Author("C", List(), "Van Rijsbergen"))
    )
    val expectedYears = List(
      Year.parse("1994"),
      Year.parse("1996"),
      Year.parse("1996")
    )
    val expectedVenues = List(
      Venue("Overview of the Third  Text REtrieval Conference (TREC"),
      Venue("The  Fourth  Text  REtrieval Conference (TREC-4)."),
      Venue("British Library  Research  and Development Report")
    )

    var i = 0
    while (i < bib.entries.size) {
      println(s"bib # $i")
      val curr: PaperMetadata = bib.entries(i)._1
      val thisExpectedAuthors = expectedAuthors(i)
      println("bib authors:")
      curr.authors.toSet.foreach(println)
      val thisExpectedYear = expectedYears(i)
      println("bib year:")
      println("\t" + curr.year)
      val thisExpectedVenue = expectedVenues(i)
      println("bib venue:")
      println("\t" + curr.venue)
      println("")

      curr.authors.toSet should equal(thisExpectedAuthors)
      curr.year should equal(thisExpectedYear)
      curr.venue should equal(thisExpectedVenue)

      i += 1
    }

  }

  //    "StructuredDoc" should "be extracted correctly" in {
  //      val doc = RPPParser.extractStructuredDocument(IoHelpers.loadXmlFile("rpp_test.xml"))
  //
  //      doc.sections(0).header should equal(Some("Introduction"))
  //      doc.sections(0).text should not startWith ("Introduction")
  //
  //      doc.sections.last.header should equal(Some("Conclusions"))
  //
  //      val b15Mentions = doc.bibliography.entries(15)._2
  //      b15Mentions.size should equal(2)
  //      val List(m1, m2) = b15Mentions.toList
  //      doc.sections(m1.sectionNumber).text.substring(m1.begin, m1.end) should equal("Lafferty et al., 2001")
  //      doc.sections(m2.sectionNumber).text.substring(m2.begin, m2.end) should equal("Lafferty et al. (2001)")
  //
  //      doc.footnotes.size should equal(4)
  //    }

  //  "Text offset" should "be computed correctly" in {
  //    import RPPParser.Implicits._
  //    import scala.collection.JavaConverters._
  //
  //    def compareOffset(xmlString: String, elementPath: String, ancestorPath: String, count: Int = 1): Unit = {
  //      val parsed = Jsoup.parse(xmlString, "", org.jsoup.parser.Parser.xmlParser())
  //      val e = parsed.select(elementPath).asScala.head
  //      val ancestor = parsed.select(ancestorPath).head
  //      val textOffsetViaXml = e.textOffset(ancestor)
  //      val textOffsetViaString = {
  //        val ancestorText = ancestor.text
  //        val elementText = e.text
  //        var index = ancestorText.indexOf(elementText)
  //        var n = count
  //        while (n > 1) {
  //          index = ancestorText.indexOf(elementText, index + 1)
  //          n -= 1
  //        }
  //        index
  //      }
  //      textOffsetViaXml should equal(textOffsetViaString)
  //    }
  //
  //    var xml = <A>
  //      base text begin
  //      <B>
  //        <C>
  //          mention
  //        </C>
  //      </B>
  //      base text end
  //    </A>
  //    compareOffset(xml.toString, "A>B>C", "A")
  //
  //    xml = <A>
  //      base text begin
  //      <B>middle mention <C>modified mention</C> </B>
  //      base text end
  //    </A>
  //    compareOffset(xml.toString, "A>B>C", "A")
  //
  //    xml = <A>
  //      base text begin
  //      <B>
  //        middle mention
  //        <C>
  //          mention
  //        </C>
  //      </B>
  //      base text end
  //    </A>
  //    compareOffset(xml.toString, "A>B>C", "A", 2)
  //
  //    xml = <A>
  //      no surrounding whitespace
  //      <B><C>mention</C></B>
  //      base text end
  //    </A>
  //    compareOffset(xml.toString, "A>B>C", "A")
  //
  //  }

}