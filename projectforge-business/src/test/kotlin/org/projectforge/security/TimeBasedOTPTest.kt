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

package org.projectforge.security

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TimeBasedOTPTest {

  private class TestData(
    val timeInSeconds: Long,
    val date: String,
    val hexVal: String,
    val totp: String,
    val crypto: String
  )

  private class TestCrypto(val seed: String, val crypto: String)

  private val testData = listOf(
    TestData(59, "1970-01-01 00:00:59", "0000000000000001", "94287082", "SHA1"),
    TestData(59, "1970-01-01 00:00:59", "0000000000000001", "46119246", "SHA256"),
    TestData(59, "1970-01-01 00:00:59", "0000000000000001", "90693936", "SHA512"),
    TestData(1111111109, "2005-03-18 01:58:29", "00000000023523EC", "07081804", "SHA1"),
    TestData(1111111109, "2005-03-18 01:58:29", "00000000023523EC", "68084774", "SHA256"),
    TestData(1111111109, "2005-03-18 01:58:29", "00000000023523EC", "25091201", "SHA512"),
    TestData(1111111111, "2005-03-18 01:58:31", "00000000023523ED", "14050471", "SHA1"),
    TestData(1111111111, "2005-03-18 01:58:31", "00000000023523ED", "67062674", "SHA256"),
    TestData(1111111111, "2005-03-18 01:58:31", "00000000023523ED", "99943326", "SHA512"),
    TestData(1234567890, "2009-02-13 23:31:30", "000000000273EF07", "89005924", "SHA1"),
    TestData(1234567890, "2009-02-13 23:31:30", "000000000273EF07", "91819424", "SHA256"),
    TestData(1234567890, "2009-02-13 23:31:30", "000000000273EF07", "93441116", "SHA512"),
    TestData(2000000000, "2033-05-18 03:33:20", "0000000003F940AA", "69279037", "SHA1"),
    TestData(2000000000, "2033-05-18 03:33:20", "0000000003F940AA", "90698825", "SHA256"),
    TestData(2000000000, "2033-05-18 03:33:20", "0000000003F940AA", "38618901", "SHA512"),
    TestData(20000000000, "2603-10-11 11:33:20", "0000000027BC86AA", "65353130", "SHA1"),
    TestData(20000000000, "2603-10-11 11:33:20", "0000000027BC86AA", "77737706", "SHA256"),
    TestData(20000000000, "2603-10-11 11:33:20", "0000000027BC86AA", "47863826", "SHA512")
  )

  private val testCryptos = listOf(
    TestCrypto(TOTP_RFC6238.seed, "SHA1"),
    TestCrypto(TOTP_RFC6238.seed32, "SHA256"),
    TestCrypto(TOTP_RFC6238.seed64, "SHA512")
  )

  @Test
  fun hexTest() {
    testData.forEach {
      Assertions.assertArrayEquals(
        TOTP_RFC6238.hexStr2Bytes(it.hexVal),
        TimeBasedOTP.hexStr2Bytes(it.hexVal)
      )
    }
  }

  @Test
  fun getOTPTest() {
    testCryptos.forEach { crypto ->
      testData.filter { crypto.crypto == it.crypto }.forEach { data ->
        val totp = TimeBasedOTP("Hmac${crypto.crypto}", 8)
        Assertions.assertEquals(
          data.hexVal,
          TimeBasedOTP.asHex(TimeBasedOTP.getStep(data.timeInSeconds * 1000)),
          "Crypto = '${crypto.crypto}, time=${data.timeInSeconds}"
        )
        Assertions.assertEquals(
          TOTP_RFC6238.generateTOTP(crypto.seed, data.hexVal, "8", "Hmac${crypto.crypto}"),
          totp.getOTP(step = TimeBasedOTP.getStep(data.timeInSeconds * 1000), secretHexKey = crypto.seed),
          "Crypto = '${crypto.crypto}, time=${data.timeInSeconds}"
        )
        Assertions.assertEquals(
          data.totp,
          totp.getOTP(step = TimeBasedOTP.getStep(data.timeInSeconds * 1000), secretHexKey = crypto.seed),
          "Crypto = '${crypto.crypto}, time=${data.timeInSeconds}"
        )
      }
    }
  }

  @Test
  fun tbOPTTest() {
    // SHA1

    testCryptos.forEach { crypto ->
      val totp = TimeBasedOTP("Hmac${crypto.crypto}", 8)
      testData.filter { it.crypto == crypto.crypto }.forEach {
        Assertions.assertEquals(
          it.hexVal,
          TimeBasedOTP.asHex(TimeBasedOTP.getStep(it.timeInSeconds * 1000))
        )
        Assertions.assertEquals(
          "${it.totp}".padStart(6, '0'),
          totp.getOTP(TimeBasedOTP.getStep(it.timeInSeconds * 1000), crypto.seed),
          "Testing crypto '${crypto.crypto}' for step ${it.timeInSeconds}"
        )
      }
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val testKey = TimeBased2FA.standard.generateSecretKey()//"DGIORGZZDGEMYJQULMOLU7U3KWIEVYBV"
      println("TestKey=$testKey")
      var lastCode = ""
      while (true) {
        val code = TimeBased2FA.standard.getTOTPCode(testKey)
        if (code != lastCode) {
          lastCode = code
          println(code)
        }
        Thread.sleep(1000)
      }
    }
  }
}
