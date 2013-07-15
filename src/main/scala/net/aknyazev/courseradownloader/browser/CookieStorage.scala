package net.aknyazev.courseradownloader.browser

import scala.collection.mutable.{HashSet, HashMap}
import java.net.{HttpURLConnection, URLConnection, HttpCookie}
import scala.collection.JavaConversions._

/**
 * Author: MrKnyaz
 * Date: 7/15/13
 */
//TODO - effective way to store and retrieve cookies
class CookieStorage {
  //Cookies map contains "domain name" as a key and a set of cookies
  private val data = new HashMap[String, HashSet[HttpCookie]]();

  //After connection has sent response
  def saveCookies(conn: URLConnection) {
    val map = conn.getHeaderFields.filter(_._1 != null).filter(_._1.equalsIgnoreCase("set-cookie"))
    if (map != null && !map.isEmpty) {
      val cookies = map.head
      for (cookie <- cookies._2.map("Set-Cookie:" + _)) {
        saveCookie("www.coursera.org", HttpCookie.parse(cookie).head)
      }
    }
  }

  def saveCookie(domain: String, cookie: HttpCookie) {
    //val cookieDomain = if (cookie.getDomain != null) cookie.getDomain else domain;
    if (!data.contains(domain)) {
      data += domain -> HashSet[HttpCookie]()
    }
    data(domain) += cookie
  }

  //Setting suitable cookies to current connection
  def setCookies(conn: HttpURLConnection) {
    val connDomain = "www.coursera.org"
    val connPath = conn.getURL.getPath
    if (data.contains(connDomain)) {
      val cookieString = new StringBuilder();
      val cookies = data(connDomain)
      for (cookie <- cookies.filter((c: HttpCookie) => c.getPath == null || connPath.startsWith(c.getPath))) {
        cookieString.append(cookie.toString + ";")
      }
      conn.addRequestProperty("Cookie", cookieString.toString())
    }
  }

}
