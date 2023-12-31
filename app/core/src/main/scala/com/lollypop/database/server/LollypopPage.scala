package com.lollypop.database.server

import com.lollypop.database.QueryResponse
import com.lollypop.database.server.LollypopPage.{LollypopPageHTMLRenderer, toHTML}
import com.lollypop.runtime.RuntimeFiles.RecursiveFileList
import com.lollypop.runtime._
import com.lollypop.runtime.datatypes.Matrix
import com.lollypop.runtime.devices.{Row, RowCollection, TableColumn}
import com.lollypop.runtime.instructions.expressions.{GraphResult, RuntimeExpression}
import com.lollypop.runtime.instructions.queryables.TableRendering
import com.lollypop.util.StringRenderHelper
import com.lollypop.util.StringRenderHelper.StringRenderer
import lollypop.io.IOCost
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

import java.io.{File, PrintWriter, StringWriter}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.io.Source

/**
 * Represents a LollypopPage; an enriched markdown document.
 * @param baseDirectory the [[File base directory]]
 * @param markdown      the contents of the markdown document
 * @param cssFiles      the optional collection of CSS paths
 */
case class LollypopPage(baseDirectory: File,
                        markdown: String,
                        cssFiles: Seq[String] = Nil) extends RuntimeExpression {

  override def execute()(implicit scope: Scope): (Scope, IOCost, String) = {
    val (_, code1) = parseTag(scope, markdown, tagStart = "<LollypopCode>", tagEnd = "</LollypopCode>")(executeLollypopCode)
    val (_, code2) = parseTag(scope, code1, tagStart = "<LollypopExample>", tagEnd = "</LollypopExample>")(executeLollypopExample)

    // render the markdown as HTML
    val parser = Parser.builder().build()
    val document = parser.parse(code2)
    val renderer = HtmlRenderer.builder().build()
    (scope, IOCost.empty, wrapHTML(renderer.render(document)))
  }

  private def wrapHTML(contents: String): String = {
    val writer = new StringWriter(contents.length + 512)
    writer.write(
      s"""|<!DOCTYPE html>
          |<html>
          |<head>
          |<title>Lollypop Pages</title>
          |${cssFiles.map(path => s"""<link href="$path" type="text/css" rel="stylesheet" media="screen">""").mkString}
          |</head>
          |""".stripMargin)
    writer.write(contents)
    writer.write("</html>")
    writer.getBuffer.toString
  }

  private def executeLollypopCode(scope0: Scope, code0: String): (Scope, String) = {
    try {
      val (scope1, result1) = invokeCode(scope0, code0)
      (scope1, result1.renderHTML(baseDirectory)(scope1))
    } catch {
      case cause: Throwable => (scope0, toHTML(cause))
    }
  }

  private def executeLollypopExample(scope0: Scope, code0: String): (Scope, String) = {
    val (scope1, result1) = invokeCode(scope0, code0)
    val writer = new StringWriter()
    //writer.write("<h4>Example</h4>")
    writer.write("<div class=\"source-code\">\n")
    writer.write(s"""<pre><code class="language-lollypop">${code0.trim}</code></pre>\n""")
    writer.write("</div>\n")
    writer.write("<h4>Results</h4>")
    writer.write(result1.renderHTML(baseDirectory)(scope1))
    val consoleOut = scope1.getUniverse.system.stdOut.asString().trim
    if (consoleOut.nonEmpty) {
      writer.write("<h4>Console Output</h4>")
      writer.write(s"""<pre><code class="console-output">$consoleOut</code></pre>""")
    }
    val consoleErr = scope1.getUniverse.system.stdErr.asString().trim
    if (consoleErr.nonEmpty) {
      writer.write("<h4>Console Error</h4>")
      writer.write(s"""<pre><code class="console-error">$consoleErr</code></pre>""")
    }
    (scope1, writer.getBuffer.toString)
  }

  private def invokeCode(scope0: Scope, code0: String): (Scope, Any) = {
    try {
      val (scope1, cost1, result1) = Await.result(Future(LollypopVM.executeSQL(scope0, code0)), 7.seconds)
      val result2 = result1 match {
        case f: Future[_] => Await.result(f, 7.seconds)
        case x => x
      }
      (scope0, result2)
    } catch {
      case cause: Throwable => (scope0, cause)
    }
  }

  private def parseTag(scope: Scope, code: String, tagStart: String, tagEnd: String)(f: (Scope, String) => (Scope, String)): (Scope, String) = {
    val markDown = new StringBuilder(code)
    var done: Boolean = false
    var last: Int = -1
    var scope0: Scope = scope
    do {
      // get the boundaries of the start and end tags
      val start = markDown.indexOf(tagStart, last)
      val end = if (start != -1) markDown.indexOf(tagEnd, start) else -1

      // if we found a tag...
      done = start == -1 || end == -1 || last >= markDown.length()
      if (!done) {
        // extract the Lollypop code and execute it
        val lollypopCode = markDown.substring(start + tagStart.length, end)
        val (scope1, result1) = f(scope0, lollypopCode)
        scope0 = scope1

        // replace the tag boundary with the evaluated output
        markDown.replace(start, end + tagEnd.length, result1)
      }
      last = start + tagStart.length
    } while (!done)
    (scope0, markDown.toString())
  }

}

object LollypopPage {

  def fromFile(baseDirectory: File, cssFiles: Seq[String] = Nil): LollypopPage = {
    val mdFile = baseDirectory / "Examples.md"
    LollypopPage(baseDirectory, markdown = Source.fromFile(mdFile).use(_.mkString), cssFiles = cssFiles)
  }

  private def toHTML(columns: Seq[TableColumn]): String = {
    val html = new StringBuilder()
    html.append("<tr>")
    columns.map(_.name).foreach(name => html.append(s"<th>$name</th>"))
    html.append("</tr>\n")
    html.toString()
  }

  private def toHTML(row: Row): String = {
    val html = new StringBuilder()
    html.append("<tr>")
    row.fields.map(_.value.orNull).foreach(value => html.append(s"<td>${value.renderFriendly}</td>"))
    html.append("</tr>\n")
    html.toString()
  }

  private def toHTML(rc: RowCollection): String = {
    val html = new StringBuilder(512)
    html.append("<table>\n")
    html.append(toHTML(rc.columns))
    rc.foreach { row =>
      html.append(toHTML(row))
    }
    html.append("</table>\n")
    html.toString()
  }

  private def toHTML(cause: Throwable): String = {
    val out = new StringWriter(256)
    out.write("""<pre><code class="console-throws">""")
    cause.printStackTrace(new PrintWriter(out))
    out.write("</code></pre>\n")
    out.flush()
    out.getBuffer.toString
  }

  private def toLines(rc: RowCollection): String = {
    val html = new StringBuilder(512)
    html.append(s"""<pre><code class="console-result">\n""")
    html.append(rc.tabulate().mkString("\n"))
    html.append("</code></pre>\n")
    html.toString()
  }

  private def toLines(rendering: TableRendering)(implicit scope: Scope): String = {
    val html = new StringBuilder(512)
    html.append(s"""<pre><code class="console-result">\n""")
    html.append(rendering.toTable.tabulate().mkString("\n"))
    html.append("</code></pre>\n")
    html.toString()
  }

  /**
   * HTML Renderer
   * @param item the [[Any value]] to render as a string
   */
  final implicit class LollypopPageHTMLRenderer(val item: Any) extends AnyVal {

    def renderHTML(baseDirectory: File)(implicit scope: Scope): String = {

      @tailrec
      def recurse(value: Any): String = value match {
        case b: Array[Byte] => recurse(StringRenderHelper.toByteArrayString(b, isPretty = false))
        case d: GraphResult => draw(d)
        case f: Future[_] => recurse(Await.result(f, 7.seconds))
        case r: TableRendering => toLines(r)
        case i: IOCost => toHTML(i.toTable)
        case m: Matrix => toHTML(m.toTable)
        case q: QueryResponse => toLines(q.toRowCollection)
        case r: Row => toHTML(r.toRowCollection)
        case r: RowCollection => toLines(r)
        case r: RowCollection => toHTML(r)
        case s: Scope => toLines(s.toRowCollection)
        case t: Throwable => toHTML(t)
        case x => s"""<pre><code class="console-result">${x.renderTable}</code></pre>\n"""
      }

      def draw(d: GraphResult): String = {
        val imageDirectory = baseDirectory / "images"
        val imageFile = LollypopChartGenerator.generateFile(imageDirectory, d)
        s"""|<div style="width: 100%">
            |<img src="${imageFile.getAbsolutePath}">
            |</div>
            |""".stripMargin
      }

      recurse(item)
    }

  }

}