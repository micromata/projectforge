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

package org.projectforge.business.ldap

import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

private val log = KotlinLogging.logger {}

/**
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 * http://stackoverflow.com/questions/3964703/can-i-add-a-new-certificate-to-the-keystore-without-restarting-the-jvm
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MySSLSocketFactory : SSLSocketFactory() {
  private val sf: SSLSocketFactory
  @Throws(IOException::class)
  override fun createSocket(): Socket {
    log.info("[createSocket()]")
    return sf.createSocket()
  }

  @Throws(IOException::class)
  override fun createSocket(host: String, port: Int): Socket {
    log.info("[createSocket(host='$host', port=$port)]")
    return sf.createSocket(host, port)
  }

  @Throws(IOException::class)
  override fun createSocket(host: InetAddress, port: Int): Socket {
    log.info("[createSocket(host='$host', port=$port)]")
    return sf.createSocket(host, port)
  }

  @Throws(IOException::class)
  override fun createSocket(s: Socket, consumed: InputStream, autoClose: Boolean): Socket {
    log.info("[createSocket(socket='${s.inetAddress}', InputStream, autoClose=$autoClose)]")
    return sf.createSocket(s, consumed, autoClose)
  }

  @Throws(IOException::class)
  override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
    log.info("[createSocket(socket='${s.inetAddress}', host='$host', port=$port, autoClose=$autoClose)]")
    return sf.createSocket(s, host, port, autoClose)
  }

  @Throws(IOException::class)
  override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
    log.info("[createSocket(host='$host', port=$port, localHost='${localHost.hostAddress}', localPort=$localPort)]")
    return sf.createSocket(host, port, localHost, localPort)
  }

  @Throws(IOException::class)
  override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
    log.info("[createSocket(address='$address', port=$port, localAddress='$localAddress', localPort=$localPort)]")
    return sf.createSocket(address, port, localAddress, localPort)
  }

  override fun getDefaultCipherSuites(): Array<String> {
    return sf.defaultCipherSuites
  }

  override fun getSupportedCipherSuites(): Array<String> {
    return sf.supportedCipherSuites
  }

  init {
    log.info("[MySSLSocketFactory]")
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, MyTrustManager.getTrustManagers(), null)
    sf = ctx.socketFactory
  }
}
