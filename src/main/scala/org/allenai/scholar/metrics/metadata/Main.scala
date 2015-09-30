package org.allenai.scholar.metrics.metadata

import java.io.File
import java.nio.file.{ Files, Paths }

object Main extends App {
  import Config._

  def runTest(): Unit = println("hello world")

  /** Run only Grobid's processHeader for now, not fullText.
    * https://github.com/kermitt2/grobid/wiki/Grobid-batch-quick-start
    */
  def runGrobid(): Unit = {
    println("RUN GROBID!!!")
    val processCmd = s"""java -Xmx4096m
                         -jar $grobidJar -gH $grobidHome
                         -gP $grobidProperties
                         -dIn $aclPdfDir
                         -dOut $grobidAclExtracted
                         -exe processFullText"""
    runProcess(processCmd)
  }

  def evalGrobid(): Unit = {
    Eval.run(
      algoName = "Grobid",
      parser = GrobidParser.extractMetadataAndBib,
      extractedDir = new File(grobidAclExtracted),
      groundTruthMetadataFile = aclMetadata,
      groundTruthCitationEdgesFile = aclCitationEdges,
      idWhiteListFile = Some(aclIdWhiteList)
    )
  }

  def runPsToText(): Unit = {
    def psToTextOutputFile(input: File): String = s"$pstotextAclExtracted/${input.getName}.xml"
    def processCmd(input: File): String =
      s"""$pstotextHome/bin/pstotext
           -output ${psToTextOutputFile(input)}
           -ligatures $pstotextHome/bin/ligatures.txt
           ${input.getAbsolutePath}"""

    val startTime = System.currentTimeMillis()
    val inputs = new File(aclPdfDir).listFiles
    inputs.foreach(input => runProcess(processCmd(input), time = false))
    println(s"Time elapsed in milliseconds: ${System.currentTimeMillis() - startTime}")
  }

  def runMetatagger(): Unit = {
    val inputs = new File(pstotextAclExtracted).listFiles
    val metataggerInput = inputs.flatMap { input =>
      val output = s"$metataggerAclExtracted/${input.getName}.tagged.xml"

      // skip if output file exists
      if (Files.exists(Paths.get(output))) None else Some(s"${input.getPath} -> $output")
    }

    runProcess(
      s"bin/runcrf",
      cwd = Some(metataggerHome),
      input = Some(metataggerInput.mkString("\n"))
    )
  }

  def evalMetatagger(): Unit = {
    Eval.run(
      algoName = "Metagagger",
      parser = MetataggerParser.parseCoreMetadataString,
      extractedDir = new File(metataggerAclExtracted),
      groundTruthMetadataFile = aclMetadata,
      groundTruthCitationEdgesFile = aclCitationEdges,
      idWhiteListFile = Some(aclIdWhiteList)
    )
  }

  def runIeslPdfToText(): Unit = {
    def processCmd(input: String, output: String) = s"$ieslPdfToTextHome/bin/run.js --svg -i $input -o $output" //$ieslPdfToTextHome/bin/run.js --svg -i $input -o $output"
    val inputs = new File(aclPdfDir).listFiles
    val startTime = System.currentTimeMillis()
    inputs.foreach { input =>
      val output = s"$ieslPdfToTextExtracted/${input.getName}.xml"
      if (!new File(output).exists) {
        runProcess(processCmd(input.getPath, output), cwd = Some(ieslPdfToTextHome), time = true)
      }
    }
  }

  def runRPP() = {
    println(ieslPdfToTextExtracted)
    val cmd = s"./batchrun.sh $rppHome file://$rppLexicons $ieslPdfToTextExtracted $rppAclExtracted"
    runProcess(cmd, cwd = Some(rppHome))
  }

  def evalRPP(): Unit = {
    Eval.run(
      algoName = "RPP",
      parser = RPPParser.extractMetadataAndBib,
      extractedDir = new File(rppAclExtracted),
      groundTruthMetadataFile = aclMetadata,
      groundTruthCitationEdgesFile = aclCitationEdges,
      idWhiteListFile = Some(aclIdWhiteList)
    )
  }

  //  def evalRPP(): Unit = {
  //    Eval(
  //      algoName = "RPP",
  //      taggedFiles = new File(rppExtracted).listFiles,
  //      taggedFileParser = RppParser.parseCoreMetadata
  //    ).run(aclMetadata, aclCitationEdges, Some(aclIdWhiteList))
  //  }
  //
  val cmds = this.getClass.getDeclaredMethods.map(m => m.getName -> m).toMap

  cmds get args(0) match {
    case Some(m) =>
      println(s"Invoking ${m.getName}")
      m.invoke(this)
    case _ => println(s"Unrecognized cmd: ${args(0)}")
  }
}
