package org.allenai.scholar.metrics.metadata

import java.io.File
import java.nio.file.{ Files, Paths }

object Main extends App {
  import Config._
  /** Run only Grobid's processHeader for now, not fullText.
    * https://github.com/kermitt2/grobid/wiki/Grobid-batch-quick-start
    */
  def runGrobid(): Unit = {
    val processCmd = s"""java -Xmx4096m
                         -jar $grobidJar -gH $grobidHome
                         -gP $grobidProperties
                         -dIn $aclPdfDir
                         -dOut $grobidAclExtracted
                         -exe processFullText"""
    runProcess(processCmd)
  }

  def evalGrobid(): Unit = {
    import org.allenai.scholar.MetadataAndBibliography
    //taggedFileParser: File => Option[MetadataAndBibliography]
    val tfp: File => Option[MetadataAndBibliography] = GrobidParser.parseCoreMetadata
    Eval(
      algoName = "Grobid",
      taggedFiles = new File(grobidAclExtracted).listFiles,
      taggedFileParser = tfp
    ).run(aclMetadata, aclCitationEdges, Some(aclIdWhiteList))
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
    Eval(
      algoName = "Metatagger",
      taggedFiles = new File(metataggerAclExtracted).listFiles,
      taggedFileParser = MetataggerParser.parseCoreMetadata
    ).run(aclMetadata, aclCitationEdges, Some(aclIdWhiteList))
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
    val cmd = s"./batchrun.sh $rppHome file://$rppLexicons $ieslPdfToTextExtracted $rppExtracted"
    runProcess(cmd, cwd = Some(rppHome))
  }

  def evalRPP(): Unit = {
    Eval(
      algoName = "RPP",
      taggedFiles = new File(rppExtracted).listFiles,
      taggedFileParser = RppParser.parseCoreMetadata
    ).run(aclMetadata, aclCitationEdges, Some(aclIdWhiteList))
  }

  val cmds = this.getClass.getDeclaredMethods.map(m => m.getName -> m).toMap

  cmds get args(0) match {
    case Some(m) =>
      println(s"Invoking ${m.getName}")
      m.invoke(this)
    case _ => println(s"Unrecognized cmd: ${args(0)}")
  }
}
