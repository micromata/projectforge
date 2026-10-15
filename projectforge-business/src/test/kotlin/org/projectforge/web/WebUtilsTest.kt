/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class WebUtilsTest {
  @Test
  fun urlNormalizingTest() {
    assertNull(WebUtils.normalizeUri(null))
    assertEquals("/", WebUtils.normalizeUri(""))
    assertEquals("/", WebUtils.normalizeUri("/"))
    assertEquals("/react/", WebUtils.normalizeUri("/react/"))
    assertEquals("/rs", WebUtils.normalizeUri("/react/../rs"))
    assertEquals("<invalid>", WebUtils.normalizeUri("../rs"))
    assertEquals("/", WebUtils.normalizeUri("/react/../rs/../"))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../.."))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../../"))
    assertEquals("<invalid>", WebUtils.normalizeUri("/react/../rs/../../react"))
    // Two dots inside a file name are no unresolved path segment: Next.js names its build chunks like this.
    assertEquals(
      "/next/_next/static/chunks/0b4s9fzw~-kl..js",
      WebUtils.normalizeUri("/next/_next/static/chunks/0b4s9fzw~-kl..js")
    )
    assertEquals("/next/chunks/a..b..c", WebUtils.normalizeUri("/next/chunks/a..b..c"))
  }

  @Test
  fun paramsAsStringTest() {
    assertEquals("", WebUtils.queryParamsToString(emptyList()))
    assertEquals("?a=1", WebUtils.queryParamsToString(listOf(Pair("a", "1"))))
    assertEquals("?a=1&b=2", WebUtils.queryParamsToString(listOf(Pair("a", "1"), Pair("b", "2"))))
  }

  @Test
  fun parseQueryParamsTest() {
    assertEquals("", WebUtils.queryParamsToString(WebUtils.parseQueryParams("url")))
    assertEquals("?id=5", WebUtils.queryParamsToString(WebUtils.parseQueryParams("url?id=5")))
    assertEquals(
      "?id=5&name=Kai+Reinhard",
      WebUtils.queryParamsToString(WebUtils.parseQueryParams("url?id=5&name=Kai%20Reinhard"))
    )
    assertEquals("?a=1%2B2", WebUtils.queryParamsToString(WebUtils.parseQueryParams("url?a=1%2B2")))
  }

  /**
   * X-Forwarded-For may be sent by any client, so it must only be believed if the request came from one of our
   * own reverse proxies: the ip address keys the brute force protection of the login and is written to all logs.
   */
  @Test
  fun clientIpTest() {
    val origin = WebUtils.trustedProxies
    try {
      WebUtils.trustedProxies = TrustedProxies("127.0.0.1,10.0.0.0/8")

      // Header of a client talking to us directly: not believed.
      assertEquals("203.0.113.7", clientIp(remoteAddr = "203.0.113.7", forwardedFor = "1.2.3.4"))
      // ... not even if it claims to come from a trusted proxy:
      assertEquals("203.0.113.7", clientIp(remoteAddr = "203.0.113.7", forwardedFor = "127.0.0.1"))

      // Header handed over by a trusted proxy: believed, first entry is the client.
      assertEquals("1.2.3.4", clientIp(remoteAddr = "127.0.0.1", forwardedFor = "1.2.3.4"))
      assertEquals("1.2.3.4", clientIp(remoteAddr = "10.11.12.13", forwardedFor = "1.2.3.4, 10.0.0.1"))
      assertEquals("1.2.3.4", clientIp(remoteAddr = "127.0.0.1", forwardedFor = " 1.2.3.4 "))
      // Outside the configured range:
      assertEquals("11.0.0.1", clientIp(remoteAddr = "11.0.0.1", forwardedFor = "1.2.3.4"))

      // A trusted proxy handing over garbage: fall back to the address of the proxy, don't do a dns lookup of a
      // header value.
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = "not-an-ip"))
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = "evil.com"))
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = ""))
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = "1.2.3.4.5"))

      // No header at all:
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = null))

      // Nothing configured: the header is never used.
      WebUtils.trustedProxies = TrustedProxies(null)
      assertEquals("127.0.0.1", clientIp(remoteAddr = "127.0.0.1", forwardedFor = "1.2.3.4"))
    } finally {
      WebUtils.trustedProxies = origin
    }
  }

  @Test
  fun trustedProxiesTest() {
    assertTrue(TrustedProxies(null).isEmpty)
    assertTrue(TrustedProxies("").isEmpty)
    assertFalse(TrustedProxies(null).isTrusted("127.0.0.1"))

    // The default of application.properties:
    val proxies = TrustedProxies("127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16")
    assertTrue(proxies.isTrusted("127.0.0.1"))
    assertTrue(proxies.isTrusted("::1"))
    assertTrue(proxies.isTrusted("10.0.0.1"))
    assertTrue(proxies.isTrusted("10.255.255.255"))
    assertTrue(proxies.isTrusted("172.16.0.1"))
    assertTrue(proxies.isTrusted("172.31.255.255"))
    assertTrue(proxies.isTrusted("192.168.42.1"))
    // Just outside the ranges (the /12 is the point of this: 172.15 and 172.32 are public):
    assertFalse(proxies.isTrusted("172.15.255.255"))
    assertFalse(proxies.isTrusted("172.32.0.1"))
    assertFalse(proxies.isTrusted("11.0.0.1"))
    assertFalse(proxies.isTrusted("192.169.0.1"))
    assertFalse(proxies.isTrusted("203.0.113.7"))
    assertFalse(proxies.isTrusted("127.0.0.2"))
    // An IPv4 address mustn't match an IPv6 range and vice versa:
    assertFalse(proxies.isTrusted("::2"))
    assertFalse(proxies.isTrusted(null))
    assertFalse(proxies.isTrusted(""))
    assertFalse(proxies.isTrusted("not-an-ip"))

    // Invalid entries are dropped, the valid ones of the same list still work:
    val partly = TrustedProxies("nonsense,127.0.0.1,10.0.0.0/99,")
    assertTrue(partly.isTrusted("127.0.0.1"))
    assertFalse(partly.isTrusted("10.0.0.1"))
  }

  private fun clientIp(remoteAddr: String, forwardedFor: String?): String? {
    val request = Mockito.mock(HttpServletRequest::class.java)
    Mockito.`when`(request.remoteAddr).thenReturn(remoteAddr)
    Mockito.`when`(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor)
    return WebUtils.getClientIp(request)
  }
}
