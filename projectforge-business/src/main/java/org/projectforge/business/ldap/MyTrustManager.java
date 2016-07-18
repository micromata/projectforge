/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

/**
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 * http://stackoverflow.com/questions/3964703/can-i-add-a-new-certificate-to-the-keystore-without-restarting-the-jvm
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyTrustManager implements X509TrustManager
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MyTrustManager.class);;

  private X509TrustManager trustManager;

  private static final TrustManager[] tmfs;

  private static final MyTrustManager tmf;

  private Certificate certificate;

  static {
    tmf = new MyTrustManager();
    tmfs = new TrustManager[1];
    tmfs[0] = tmf;
  }

  public static final MyTrustManager getInstance()
  {
    return tmf;
  }

  public static final TrustManager[] getTrustManagers()
  {
    return tmfs;
  }

  public MyTrustManager()
  {
    try {
      final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      // create a TrustManager using our KeyStore
      final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init(keyStore);
      this.trustManager = getX509TrustManager(factory.getTrustManagers());
    } catch (final KeyStoreException ex) {
      log.error("Exception encountered " + ex, ex);
    } catch (final NoSuchAlgorithmException ex) {
      log.error("Exception encountered " + ex, ex);
    } catch (final CertificateException ex) {
      log.error("Exception encountered " + ex, ex);
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
    }
  }

  public void addCertificate(final String alias, final File file)
  {
    FileInputStream fis = null;
    try {
      log.info("Trying to add new certificate '" + alias + "': " + file);
      fis = new java.io.FileInputStream(file);
      addCertificate(alias, fis);
      fis.close();
    } catch (final FileNotFoundException ex) {
      log.error("Exception encountered " + ex, ex);
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  public void addCertificate(final String alias, final InputStream is)
  {
    CertificateFactory factory;
    try {
      factory = CertificateFactory.getInstance("X.509");
      certificate = factory.generateCertificate(is);
      // keyStore.setCertificateEntry(alias, certificate);
    } catch (final CertificateException ex) {
      log.error("Exception encountered " + ex + " while adding certificate '" + alias + "'", ex);
      // } catch (final KeyStoreException ex) {
      // log.error("Exception encountered " + ex + " while adding certificate '" + alias + "'", ex);
    }
  }

  public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException
  {
    trustManager.checkClientTrusted(chain, authType);
  }

  public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException
  {
    if (certificate != null) {
      try {
        chain[0].verify(certificate.getPublicKey());
        for (final X509Certificate cert : chain) {
          // Verifing by public key
          cert.checkValidity();
        }
      } catch (final InvalidKeyException ex) {
        throw new CertificateException(ex);
      } catch (final NoSuchAlgorithmException ex) {
        throw new CertificateException(ex);
      } catch (final NoSuchProviderException ex) {
        throw new CertificateException(ex);
      } catch (final SignatureException ex) {
        throw new CertificateException(ex);
      }
    } else {
      trustManager.checkServerTrusted(chain, authType);
    }
  }

  public X509Certificate[] getAcceptedIssuers()
  {
    return trustManager.getAcceptedIssuers();
  }

  private static X509TrustManager getX509TrustManager(final TrustManager[] managers)
  {
    for (final TrustManager tm : managers) {
      if (tm instanceof X509TrustManager) {
        return (X509TrustManager) tm;
      }
    }
    return null;
  }
}
