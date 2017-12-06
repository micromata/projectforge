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

package org.projectforge.common;

import static org.testng.AssertJUnit.*;

import java.math.BigDecimal;
import java.util.Locale;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class NumberHelperTest extends AbstractTestBase
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NumberHelperTest.class);

  @Test
  public void greaterZero()
  {
    assertEquals(false, NumberHelper.greaterZero((Integer) null));
    assertEquals(false, NumberHelper.greaterZero(-1));
    assertEquals(false, NumberHelper.greaterZero(0));
    assertEquals(true, NumberHelper.greaterZero(1));
    assertEquals(true, NumberHelper.greaterZero(100));
  }

  @Test
  public void add()
  {
    assertEquals("0", NumberHelper.add(null, null).toString());
    assertEquals("1", NumberHelper.add(BigDecimal.ONE, null).toString());
    assertEquals("1", NumberHelper.add(null, BigDecimal.ONE).toString());
    assertEquals("11", NumberHelper.add(BigDecimal.TEN, BigDecimal.ONE).toString());
  }

  @Test
  public void extractPhonenumber()
  {
    assertEquals(null, NumberHelper.extractPhonenumber(null, null));
    assertEquals("", NumberHelper.extractPhonenumber("", "+49"));
    assertEquals("", NumberHelper.extractPhonenumber("+", "+49"));
    assertEquals("4", NumberHelper.extractPhonenumber("+4", "+49"));
    assertEquals("0", NumberHelper.extractPhonenumber("+49", "+49"));
    assertEquals("01", NumberHelper.extractPhonenumber("+491", "+49"));
    assertEquals("05613167930", NumberHelper.extractPhonenumber("0561 / 316793-0", null));
    assertEquals("00495613167930", NumberHelper.extractPhonenumber("+49 561 / 316793-0", null));
    assertEquals("05613167930", NumberHelper.extractPhonenumber("+49 561 / 316793-0", "+49"));
    assertEquals("00445613167930", NumberHelper.extractPhonenumber("+44 561 / 316793-0", "+49"));
    assertEquals("004456131", NumberHelper.extractPhonenumber("+44 561 / 31:6793-0", "+49"));
    assertEquals("004456131", NumberHelper.extractPhonenumber("+44 561 / 31 h6793-0", "+49"));
  }

  @Test
  public void splitToInts()
  {
    compareIntArray(new int[] { 1, 111, 5, 11 }, NumberHelper.splitToInts(11110511, 1, 3, 2, 2));
    compareIntArray(new int[] { 1, 0, 5, 11 }, NumberHelper.splitToInts(10000511, 1, 3, 2, 2));
    compareIntArray(new int[] { 0, 0, 5, 11 }, NumberHelper.splitToInts(511, 1, 3, 2, 2));
    compareIntArray(new int[] { 0, 0, 5, 11 }, NumberHelper.splitToInts(511, 1, 3, 2, 2));
    compareIntArray(new int[] { 5, 120, 1, 2 }, NumberHelper.splitToInts(new Double("51200102"), 1, 3, 2, 2));
  }

  @Test
  public void toPlainString()
  {
    assertEquals("20070206001", NumberHelper.toPlainString("2.0070206001E10"));
    assertEquals("", NumberHelper.toPlainString(""));
    assertEquals(" ", NumberHelper.toPlainString(" "));
    assertEquals("1", NumberHelper.toPlainString("1"));
    assertEquals("hallo1", NumberHelper.toPlainString("hallo1"));
  }

  @Test
  public void isBigDecimalEqual()
  {
    assertEquals(true, NumberHelper.isEqual((BigDecimal) null, (BigDecimal) null));
    assertEquals(false, NumberHelper.isEqual(null, BigDecimal.ZERO));
    assertEquals(false, NumberHelper.isEqual(BigDecimal.ZERO, null));
    assertEquals(true, NumberHelper.isEqual(BigDecimal.ZERO, new BigDecimal("0.0")));
    assertEquals(true, NumberHelper.isEqual(new BigDecimal("1.5").setScale(1), new BigDecimal("1.50").setScale(2)));
    assertEquals(true,
        NumberHelper.isEqual(new BigDecimal("-891.5").setScale(1), new BigDecimal("-891.50").setScale(2)));
  }

  @Test
  public void isIntegerNotZero()
  {
    assertEquals(false, NumberHelper.isNotZero((Integer) null));
    assertEquals(false, NumberHelper.isNotZero(0));
    assertEquals(true, NumberHelper.isNotZero(1));
  }

  @Test
  public void isBigDecimalNotZero()
  {
    assertEquals(false, NumberHelper.isNotZero((BigDecimal) null));
    assertEquals(false, NumberHelper.isNotZero(BigDecimal.ZERO));
    assertEquals(false, NumberHelper.isNotZero(new BigDecimal("0").setScale(3)));
    assertEquals(true, NumberHelper.isNotZero(new BigDecimal("1")));
  }

  @Test
  public void isBigDecimalZeroOrNull()
  {
    assertEquals(true, NumberHelper.isZeroOrNull((BigDecimal) null));
    assertEquals(true, NumberHelper.isZeroOrNull(BigDecimal.ZERO));
    assertEquals(true, NumberHelper.isZeroOrNull(new BigDecimal("0").setScale(3)));
    assertEquals(false, NumberHelper.isZeroOrNull(new BigDecimal("1")));
  }

  @Test
  public void isIntegerEqual()
  {
    assertEquals(true, NumberHelper.isEqual((Integer) null, (Integer) null));
    assertEquals(false, NumberHelper.isEqual(null, 0));
    assertEquals(false, NumberHelper.isEqual(0, null));
    assertEquals(true, NumberHelper.isEqual(0, 0));
    assertEquals(true, NumberHelper.isEqual(new Integer(42), 42));
    assertEquals(true, NumberHelper.isEqual(-891, new Integer("-891")));
  }

  @Test
  public void formatBytes()
  {
    final PFUserDO user = new PFUserDO();
    user.setLocale(Locale.UK);
    ThreadLocalUserContext.setUser(getUserGroupCache(), user);
    assertEquals("0 bytes", NumberHelper.formatBytes(0));
    assertEquals("1023 bytes", NumberHelper.formatBytes(1023));
    assertEquals("1 kb", NumberHelper.formatBytes(1024));
    assertEquals("1 kb", NumberHelper.formatBytes(1075));
    assertEquals("1.1 kb", NumberHelper.formatBytes(1076));
    assertEquals("99.9 kb", NumberHelper.formatBytes(102297));
    assertEquals("1,023 kb", NumberHelper.formatBytes(1047552));
    assertEquals("1 Mb", NumberHelper.formatBytes(1048576));
    assertEquals("1 Gb", NumberHelper.formatBytes(1073741824));
  }

  @Test
  public void setDefaultScale()
  {
    assertEquals(2, NumberHelper.setDefaultScale(new BigDecimal(0.76274327)).scale());
    assertEquals(2, NumberHelper.setDefaultScale(new BigDecimal(19.999)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(20)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(20.000001)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(99.99999)).scale());
    assertEquals(0, NumberHelper.setDefaultScale(new BigDecimal(100)).scale());
    assertEquals(0, NumberHelper.setDefaultScale(new BigDecimal(100.000001)).scale());
    assertEquals(2, NumberHelper.setDefaultScale(new BigDecimal(-0.76274327)).scale());
    assertEquals(2, NumberHelper.setDefaultScale(new BigDecimal(-19.999)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(-20)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(-20.000001)).scale());
    assertEquals(1, NumberHelper.setDefaultScale(new BigDecimal(-99.99999)).scale());
    assertEquals(0, NumberHelper.setDefaultScale(new BigDecimal(-100)).scale());
    assertEquals(0, NumberHelper.setDefaultScale(new BigDecimal(-100.000001)).scale());
  }

  @Test
  public void randomTest()
  {
    for (int i = 0; i < 10; i++) {
      final String randomString = NumberHelper.getSecureRandomUrlSaveString(20);
      log.info(randomString);
    }
    for (int i = 1; i < 20; i++) {
      final String randomString = NumberHelper.getSecureRandomBase64String(i);
      log.info(randomString);
    }
  }

  @Test
  public void isIn()
  {
    assertFalse(NumberHelper.isIn(42));
    assertFalse(NumberHelper.isIn(42, 0));
    assertTrue(NumberHelper.isIn(42, 42));
    assertTrue(NumberHelper.isIn(0, 0));
    assertTrue(NumberHelper.isIn(0, 1, 0));
  }

  private void compareIntArray(final int[] a1, final int[] a2)
  {
    assertEquals(a1.length, a2.length);
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }
  }
}
