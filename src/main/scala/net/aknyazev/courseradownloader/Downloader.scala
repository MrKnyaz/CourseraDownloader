package net.aknyazev.courseradownloader

import net.aknyazev.courseradownloader.browser.Browser
import scala.io.Source

/**
 * Author: MrKnyaz
 * Date: 7/14/13
 */
object Downloader extends App {
  println("hello")
  var browser = new Browser("email", "password");
  //course name, first video, last video, videos(true or false), subtitles, pdfs
  browser.downloadVideos("progfun-002", 0, 10000, true, true, true)
}
