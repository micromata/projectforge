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

package org.projectforge.framework.utils;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Wolfgang Jung (W.Jung@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class Crypt
{
  private final static Logger log = LoggerFactory.getLogger(Crypt.class);

  private static final String CRYPTO_ALGORITHM = "AES/ECB/PKCS5Padding";

  private static boolean initialized;

  /**
   * Encrypts the given str with AES. The password is first converted using SHA-256.
   * 
   * @param password
   * @param str
   * @return The base64 encoded result (url safe).
   */
  public static String encrypt(final String password, final String data)
  {
    initialize();
    try {
      // AES is sometimes not part of Java, therefore use bouncy castle provider:
      final Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
      final byte[] keyValue = getPassword(password);
      final Key key = new SecretKeySpec(keyValue, "AES");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      final byte[] encVal = cipher.doFinal(data.getBytes("UTF-8"));
      final String encryptedValue = Base64.encodeBase64URLSafeString(encVal);
      return encryptedValue;
    } catch (final Exception ex) {
      log.error("Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
          + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * @param password
   * @param encryptedString
   * @return
   */
  public static String decrypt(final String password, final String encryptedString)
  {
    initialize();
    try {
      final Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
      final byte[] keyValue = getPassword(password);
      final Key key = new SecretKeySpec(keyValue, "AES");
      cipher.init(Cipher.DECRYPT_MODE, key);
      final byte[] decordedValue = Base64.decodeBase64(encryptedString);
      final byte[] decValue = cipher.doFinal(decordedValue);
      final String decryptedValue = new String(decValue, "UTF-8");
      return decryptedValue;
    } catch (BadPaddingException bpe) {
      log.warn(bpe.getMessage());
      return null;
    } catch (final Exception ex) {
      log.error("Exception encountered while trying to encrypt with Algorithm 'AES' and the given password: "
          + ex.getMessage(), ex);
      return null;
    }
  }

  private static byte[] getPassword(final String password)
  {
    try {
      final MessageDigest digester = MessageDigest.getInstance("MD5"); // 128 bit. 256 bit (SHA-256) doesn't work on Java versions without required security policy.
      digester.update(password.getBytes("UTF-8"));
      final byte[] key = digester.digest();
      return key;
    } catch (final NoSuchAlgorithmException ex) {
      log.error("Exception encountered while trying to create a MD5 password: " + ex.getMessage(), ex);
      return null;
    } catch (final UnsupportedEncodingException ex) {
      log.error("Exception encountered while trying to get bytes in UTF-8: " + ex.getMessage(), ex);
      return null;
    }
  }

  private static void initialize()
  {
    synchronized (log) {
      if (initialized == false) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        initialized = true;
      }
    }
  }

  /**
   * Encrypts the given String via SHA crypt algorithm.
   * 
   * @param s
   * @return
   */
  public static String digest(final String s)
  {
    return encode(s, "SHA");
  }

  public static String digest(final String s, final String alg)
  {
    return encode(s, alg);
  }

  public static boolean check(final String pass, final String encoded)
  {
    final String alg = encoded.substring(0, encoded.indexOf('{'));
    return encoded.equals(encode(pass, alg));
  }

  private static String encode(final String s, final String alg)
  {
    try {
      final MessageDigest md = MessageDigest.getInstance(alg);
      md.reset();
      md.update(s.getBytes());
      final byte[] d = md.digest();

      String ret = "";

      for (int val : d) {
        final char[] hex = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F' };
        if (val < 0) {
          val = 256 + val;
        }
        final char hi = hex[val / 16];
        final char lo = hex[val % 16];
        ret = hi + "" + lo + ret;
      }
      return md.getAlgorithm() + '{' + ret + '}';
    } catch (final NoSuchAlgorithmException ex) {
      log.error(ex.toString());
      return "NONE{" + s + "}";
    }
  }
}
