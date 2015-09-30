package org.allenai.scholar.metrics.metadata

import com.typesafe.config.ConfigFactory

import java.io.File

object Config {
  val config = ConfigFactory.load()
  val root = config.getString("root")
  val dataHome = s"$root/${config.getString("data.home")}"
  val aclHome = s"$dataHome/${config.getString("data.acl.home")}"
  val aclPdfDir = s"$aclHome/${config.getString("data.acl.pdfDirectory")}"
  val aclMetadata = s"$aclHome/${config.getString("data.acl.metadata")}"
  val aclCitationEdges = s"$aclHome/${config.getString("data.acl.citationEdges")}"
  val aclIdWhiteList = s"$aclHome/${config.getString("data.acl.idWhiteList")}"

  println(s"Config: root = $root")
  println(s"Config: dataHome = $dataHome")

  val grobidRoot = s"$root/${config.getString("grobid.root")}"
  val grobidHome = s"$grobidRoot/grobid-home"
  lazy val grobidJar = new File(s"$grobidRoot/grobid-core/target")
    .listFiles
    .filter(_.getName.endsWith("one-jar.jar"))
    .head
    .getAbsolutePath
  val grobidProperties = s"$grobidHome/config/grobid.properties"

  val pstotextHome = s"$root/${config.getString("pstotext.home")}"
  val metataggerHome = s"$root/${config.getString("metatagger.home")}"
  val aclExtracted = s"$aclHome/${config.getString("data.acl.extracted")}"
  val grobidAclExtracted = s"$aclExtracted/grobid"
  val pstotextAclExtracted = s"$aclExtracted/pstotext"
  val metataggerAclExtracted = s"$aclExtracted/metatagger"

  /* iesl-pdf-to-text*/
  val ieslPdfToTextHome = s"$root/${config.getString("ieslPdfToText.home")}"
  val ieslPdfToTextExtracted = s"$aclExtracted/iesl-pdf-to-text"

  /* rpp */
  val rppHome = s"$root/${config.getString("rpp.home")}"
  val rppAclExtracted = s"$aclExtracted/rpp"
  val rppLexicons = s"$root/${config.getString("rpp.lexicons")}"

  val verboseLabelFormat = config.getBoolean("verboseLabelFormat")
}
