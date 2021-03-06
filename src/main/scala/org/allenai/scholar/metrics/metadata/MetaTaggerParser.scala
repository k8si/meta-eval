package org.allenai.scholar.metrics.metadata

import java.io.File
import java.time.Year

import org.allenai.scholar._
import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }
import org.jsoup.select.Elements

import scala.io.Source

/** A collection of text processing, normalization, and
  * Jsoup-based utilities packaged in implicits to reduce boiler plate.
  */

import org.allenai.scholar.metrics.metadata.Parser.ElementsImplicit.elementsToSeq
import org.allenai.scholar.metrics.metadata.Parser.JsoupElementsImplicits

abstract class Parser(
    titlePath: String,
    venuePath: String,
    yearPath: String,
    authorPath: String,
    lastRelativePath: String,
    firstRelativePath: String,
    middleRelativePath: String,
    bibMainPath: String,
    bibAuthorPath: String,
    bibTitlePath: String
) {

  def extractBibYear(bib: Element): Year

  def extractVenue(bib: Element): String

  def extractHeaderYear(elmt: Element): Year

  def extractSpecialBib(bib: Element): PaperMetadata

  protected def extractNames(e: Element, authorPath: String, initial: Boolean = false) =
    e.select(authorPath).map(a => {
      val last = a.extractName(lastRelativePath)
      val first = a.extractName(firstRelativePath)
      val middle = a.extractName(middleRelativePath)
      //      println(s"extractNames: got: first=$first middle=$middle last=$last")
      Author(first, if (middle.size > 0) List(middle) else List(), last)
    }).map(_.ifDefined).flatten.toList

  private def extractBibs(doc: Document): Seq[PaperMetadata] = {
    val main = doc.select(bibMainPath)
    main.map { bib =>
      val titleOpt = bib.extractBibTitle(bibTitlePath)
      //      println("extractBibs: got title: " + titleOpt.toString)
      titleOpt match {
        case title if title.nonEmpty =>
          PaperMetadata(
            title = Title(title),
            authors = extractNames(bib, bibAuthorPath),
            venue = Venue(extractVenue(bib)),
            year = extractBibYear(bib)
          )
        case _ => extractSpecialBib(bib)
      }
    }
  }

  //  private def extractBibs(doc: Document): Seq[PaperMetadata] =
  //    doc.select(bibMainPath).map(bib =>
  //      bib.extractBibTitle(bibTitlePath) match {
  //        case title if title.nonEmpty =>
  //          PaperMetadata(
  //            title = Title(title),
  //            authors = extractNames(bib, bibAuthorPath),
  //            venue = Venue(extractVenue(bib)),
  //            year = extractBibYear(bib)
  //          )
  //        case _ => extractSpecialBib(bib)
  //      }).toList

  def parseCoreMetadata(file: File): Option[MetadataAndBibliography] = try {
    var xmlString: String = ""
    try {
      xmlString = Source.fromFile(file, "UTF-8").getLines().mkString("\n")
    } catch {
      case e: Exception => println(s"Exception parsing file: ${file.getAbsolutePath}")
    }
    Some(parseCoreMetadataString(xmlString))
  } catch {
    case e: Exception =>
      println(s"Could not parse xml file ${file.getName}")
      e.printStackTrace()
      None
  }

  /** Function that parses XML to produce core metadata.
    * Names are lower-cased and trimmed of non-letter at the end.
    * @param xmlString The XML data as a string
    * @return The paper's core metadata.
    */
  def parseCoreMetadataString(xmlString: String): MetadataAndBibliography = {
    val doc = Jsoup.parse(xmlString, "", org.jsoup.parser.Parser.xmlParser())
    val metadata = PaperMetadata(
      title = Title(doc.extractTitle(titlePath)),
      authors = extractNames(doc, authorPath),
      year = extractHeaderYear(doc),
      venue = Venue(extractVenue(doc))
    )
    MetadataAndBibliography(
      metadata = metadata,
      bibs = extractBibs(doc)
    )
  }
}

object MetataggerParser extends Parser(
  titlePath = "document>content>headers>title",
  venuePath = "???",
  yearPath = "???",
  authorPath = "document>content>headers>authors>author",
  lastRelativePath = "author-last",
  firstRelativePath = "author-first",
  middleRelativePath = "author-middle",
  bibMainPath = "biblio>reference",
  bibAuthorPath = "authors>author",
  bibTitlePath = "title"
) {

  override def extractBibYear(bib: Element): Year = bib.extractYear("date", _.text)

  // TODO/NotYetImplemented
  override def extractHeaderYear(elmt: Element): Year = yearZero

  override def extractVenue(bib: Element): String =
    List("conference", "journal", "booktitle")
      .find(vt => !bib.select(vt).isEmpty) match {
        case Some(v) => bib.extractBibTitle(v)
        case None => ""
      }

  override def extractSpecialBib(bib: Element): PaperMetadata = {
    val metadata = PaperMetadata(
      title = Title(bib.extractBibTitle("booktitle")),
      authors = extractNames(bib, "authors>author"),
      venue = Venue(""),
      year = extractBibYear(bib)
    )
    metadata
  }
}

/*
object GrobidParser extends Parser(
  titlePath = "teiHeader>fileDesc>titleStmt>title",
  venuePath = "teiHeader>fileDesc>sourceDesc>biblStruct>monogr>title",
  yearPath = "teiHeader>fileDesc>sourceDesc>biblStruct>monogr>imprint",
  authorPath = "teiHeader>fileDesc>sourceDesc>biblStruct>analytic>author",
  lastRelativePath = "persName>surname",
  firstRelativePath = "persName>forename[type=first]",
  middleRelativePath = "persName>forename[type=middle]",
  bibMainPath = "listBibl>biblStruct",
  bibAuthorPath = "analytic>author",
  bibTitlePath = "analytic>title[type=main]"
) {
 */

object RppParser extends Parser(
  titlePath = "document>header>title",
  venuePath = "",
  yearPath = "date>year",
  authorPath = "document>header>authors>person",
  lastRelativePath = "person-last",
  firstRelativePath = "person-first",
  middleRelativePath = "person-middle",
  bibMainPath = "references>reference",
  bibAuthorPath = "authors>person",
  bibTitlePath = "title"
) {

  override def extractHeaderYear(elmt: Element): Year = elmt.extractYear("date", _.text)

  def extractBibYear(bib: Element): Year = bib.extractYear("date", _.text)

  def extractVenue(bib: Element): String =
    List("journal", "booktitle", "institution")
      .find(vt => !bib.select(vt).isEmpty) match {
        case Some(v) =>
          println("RppParser: extractVenue: vt = " + v.toString)
          bib.extractBibTitle(v)
        case None => ""
      }

  def extractSpecialBib(bib: Element): PaperMetadata = {
    val metadata = PaperMetadata(
      title = Title(bib.extractBibTitle("booktitle")),
      authors = extractNames(bib, "authors>person"),
      venue = Venue(""),
      year = extractBibYear(bib)
    )
    metadata
  }
}

object Parser {

  import org.allenai.scholar.StringUtils._

  object ElementsImplicit {

    import scala.collection.JavaConverters._
    import scala.language.implicitConversions

    /** Convenient implicit: no need for .asScala any time calling Jsoup's select. */
    implicit def elementsToSeq(elms: Elements): collection.mutable.Buffer[Element] = elms.asScala
  }

  implicit class JsoupElementsImplicits(e: Element) {

    import ElementsImplicit._

    def extract(path: String): String = {
      val thing = e.select(path)
      val result = thing.headOption match {
        case Some(v) => v.text
        case None => ""
      }
      //      println("extract: got result: " + result)
      result
    }

    def extractName(namePath: String): String = extract(namePath).trimNonAlphabetic()

    def extractTitle(path: String): String = extract(path)

    def extractBibTitle(path: String): String = e.extract(path).trimChars(",.")

    def extractYear(path: String, get: Element => String): Year =
      e.select(path).headOption match {
        case Some(d) => get(d).extractYear
        case None => yearZero
      }

  }

}
