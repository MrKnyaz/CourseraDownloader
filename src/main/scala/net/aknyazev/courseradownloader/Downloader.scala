package net.aknyazev.courseradownloader

import net.aknyazev.courseradownloader.browser.Browser

/**
 * Author: MrKnyaz
 * Date: 7/14/13
 */
object Downloader extends App {
  println("hello")
  var browser = new Browser("email", "password");
  //course name, first video, last video, videos(true or false), subtitles, pdfs
  //Every parameter is optional except course
  browser.downloadVideos(course =  "ml-003")
  browser.downloadVideos(course =  "progfun-002", pdfs = false)
}
