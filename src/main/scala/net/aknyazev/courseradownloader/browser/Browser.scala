package net.aknyazev.courseradownloader.browser

import scala.collection.mutable._
import java.net.{HttpURLConnection, URL, URLConnection, HttpCookie}
import java.io._
import scala.io.Source

/**
 * Author: MrKnyaz
 * Date: 7/14/13
 */
//Coursera browser class
class Browser(email: String, password: String) {
  private val cookieStorage = new CookieStorage
  login();

  private def login() {
    cookieStorage.saveCookie("www.coursera.org", new HttpCookie("csrftoken", "L2IrwcaoQDa3UHbt1Qpp"))
    val conn = getConnection("https://www.coursera.org/maestro/api/user/login");
    setCommonRequestProperties(conn);
    conn.setRequestProperty("Referer", "https://www.coursera.org/account/signin")
    conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    conn.addRequestProperty("X-CSRFToken", "L2IrwcaoQDa3UHbt1Qpp")
    cookieStorage.setCookies(conn);
    conn.setRequestMethod("POST")
    val form_data = s"email_address=$email&password=$password"
    conn.addRequestProperty("Content-Length", Integer.toString(form_data.length))
    conn.setDoOutput(true)
    val out: OutputStreamWriter = new OutputStreamWriter(conn.getOutputStream)
    out.write(form_data)
    out.flush
    out.close
    cookieStorage.saveCookies(conn)
  }

  private def getConnection(url: String) = {
    new URL(url).openConnection().asInstanceOf[HttpURLConnection];
  }

  private def setCommonRequestProperties(conn: HttpURLConnection) {
    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8")
    conn.addRequestProperty("User-Agent", "Mozilla")
    if (conn.getRequestProperty("Referer") == null) conn.addRequestProperty("Referer", "https://www.coursera.org")
  }

  //some urls do redirects serveral times until we get necessary connection
  private def connectUntilOK(conn: HttpURLConnection): HttpURLConnection = {
    setCommonRequestProperties(conn)
    cookieStorage.setCookies(conn)
    conn.connect()
    cookieStorage.saveCookies(conn)
    if (conn.getResponseCode == 302) {
      val nextConn = getConnection(conn.getHeaderField("Location"))
      nextConn.addRequestProperty("Referer", conn.getURL.toString)
      return connectUntilOK(nextConn)
    }
    return conn
  }

  //Parse page content and get urls to videos and subtitles
  private def parseHtml(conn: HttpURLConnection): (Iterator[String], Iterator[String], Iterator[String]) = {
    val content = Source.fromInputStream(conn.getInputStream).getLines().mkString
    val writer = new PrintWriter("content.txt")
    writer.write(content)
    writer.close()
    val videoPattern = """(<a[^>]*href="([^>]*?mp4.*?)".*?>.*?</a>)""".r
    val srtPattern = """(<a[^>]*href="([^>]*?srt.*?)".*?>.*?</a>)""".r
    val pdfPattern = """(<a[^>]*href="([^>]*?pdf.*?)".*?>.*?</a>)""".r
    val videos = for (videoPattern(anchor, url) <- videoPattern.findAllMatchIn(content)) yield url
    val srts = for (srtPattern(anchor, url) <- srtPattern.findAllMatchIn(content)) yield url
    val pdfs = for (pdfPattern(anchor, url) <- pdfPattern.findAllMatchIn(content)) yield url

    return (videos, srts, pdfs);
  }

  //public methods

  //actual download, automatically creates folder with course name and downloads all videos
  def downloadVideos(course: String, begin: Int = 0, end: Int = 10000, videos: Boolean, subtitles: Boolean, pdfs: Boolean) {
    connectUntilOK(getConnection(s"https://class.coursera.org/$course/auth/auth_redirector?type=login&subtype=normal")).disconnect()
    val conn = connectUntilOK(getConnection(s"https://class.coursera.org/$course/lecture/index"))
    //get download links for mp4, srt and pdf
    val downloadLinks = parseHtml(conn)
    //creating directory if not exists
    val courseDir: File = new File(course)
    if (!courseDir.exists()) courseDir.mkdir()
    //downloading videos
    if (videos) {
      for (url <- downloadLinks._1.zipWithIndex if url._2 >= begin && url._2 <= end) {
        val conn = connectUntilOK(getConnection(url._1))
        val contentSize = conn.getHeaderField("Content-Length").toLong
        val videoName = conn.getHeaderField("Content-Disposition").split("=")(1).replace("\"", "")
        println("Downloading video: " + videoName + "  with size: " + contentSize)
        val fileInput = conn.getInputStream
        val fileOutput = new FileOutputStream(new File(courseDir.getAbsolutePath+File.separator+videoName))
        var downloadedLength = 0;
        var partsForProgress = 20;
        val buffer = new Array[Byte](8192)
        var length: Int = fileInput.read(buffer)
        while (length != -1) {
          downloadedLength += length
          if (contentSize / downloadedLength < partsForProgress) {
            print(5*(20-partsForProgress+1)+"% ")
            partsForProgress -= 1
          }
          fileOutput.write(buffer, 0, length)
          length = fileInput.read(buffer)

        }
        fileOutput.flush
        fileOutput.close()
        println("done...")
      }
    }

  }
}

