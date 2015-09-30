//package org.allenai.scholar.metrics.metadata
//
///** @author Kate Silverstein
//  *         created on 4/8/15
//  */
//
//import org.scalatest.FlatSpec
//
//import scala.io.Source
//
//class TestExtraction extends FlatSpec {
//  val groundTruthFile = getClass().getResource("/gold-metadata").getPath
//  val citationEdgesFile = getClass().getResource("/citation-edges").getPath
//  val inputFile = getClass().getResource("/W06-0705.xml").getPath
//  "Eval" should "be perfect" in {
//    import org.allenai.scholar.PaperMetadata._
//    val groundTruth = fromJsonLinesFile(groundTruthFile)
//    val citationEdges = for {
//      line <- Source.fromFile(citationEdgesFile).getLines.toIterable
//      s = line.split('\t')
//      if s.length > 1
//    } yield { (s(0), s(1)) }
//    val bibs = org.allenai.scholar.MetadataAndBibliography.edgesToBibKeyMap(citationEdges, groundTruth)
//    val taggedFiles = Array(new java.io.File(inputFile))
//    val errAnalysis = Eval(
//      algoName = "test",
//      taggedFiles = taggedFiles,
//      taggedFileParser = RppParser.parseCoreMetadata
//    ).computeEval(groundTruth, bibs, id => true)
//    for (e <- errAnalysis) {
//      println(e.toString)
//    }
//    //    println(errAnalysis.toString)
//
//  }
//
//}
