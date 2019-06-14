/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 * http://stackoverflow.com/questions/3964703/can-i-add-a-new-certificate-to-the-keystore-without-restarting-the-jvm
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MySSLSocketFactory
{
  private final SSLSocketFactory sf;

  private static MySSLSocketFactory defaultInstance;

  public static MySSLSocketFactory getDefault() throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException,
  KeyManagementException, CertificateException, IOException
  {
    if (defaultInstance == null) {
      defaultInstance = new MySSLSocketFactory();
    }
    return defaultInstance;
  }

  public MySSLSocketFactory() throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, KeyManagementException,
  CertificateException, IOException
  {

    final SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, MyTrustManager.getTrustManagers(), null);
    sf = ctx.getSocketFactory();
  }

  public Socket createSocket() throws IOException
  {
    return sf.createSocket();
  }

  public Socket createSocket(final InetAddress arg0, final int arg1, final InetAddress arg2, final int arg3) throws IOException
  {
    return sf.createSocket(arg0, arg1, arg2, arg3);
  }

  public Socket createSocket(final InetAddress arg0, final int arg1) throws IOException
  {
    return sf.createSocket(arg0, arg1);
  }

  public Socket createSocket(final Socket s, final String host, final int port, final boolean autoClose) throws IOException
  {
    return sf.createSocket(s, host, port, autoClose);
  }

  public Socket createSocket(final String arg0, final int arg1, final InetAddress arg2, final int arg3) throws IOException,
  UnknownHostException
  {
    return sf.createSocket(arg0, arg1, arg2, arg3);
  }

  public Socket createSocket(final String arg0, final int arg1) throws IOException, UnknownHostException
  {
    return sf.createSocket(arg0, arg1);
  }

  @Override
  public boolean equals(final Object obj)
  {
    return sf.equals(obj);
  }

  public String[] getDefaultCipherSuites()
  {
    return sf.getDefaultCipherSuites();
  }

  public String[] getSupportedCipherSuites()
  {
    return sf.getSupportedCipherSuites();
  }

  @Override
  public int hashCode()
  {
    return sf.hashCode();
  }

  @Override
  public String toString()
  {
    return sf.toString();
  }
}
