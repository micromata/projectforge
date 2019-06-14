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

package org.projectforge.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StringHelperTest {
  @Test
  public void append() {
    final StringBuffer buf = new StringBuffer();
    boolean first = StringHelper.append(buf, true, null, ",");
    first = StringHelper.append(buf, first, "", ",");
    first = StringHelper.append(buf, first, "1", ",");
    first = StringHelper.append(buf, first, "2", ",");
    assertEquals("1,2", buf.toString());
  }

  @Test
  public void listToString() {
    final List<String> list = new ArrayList<String>();
    list.add("Micromata");
    list.add("Computer");
    list.add("IT-Services");
    assertEquals("Micromata,Computer,IT-Services", StringHelper.listToString(list, ",", false));
    assertEquals("Computer, IT-Services, Micromata", StringHelper.listToString(list, ", ", true));
    assertEquals("Micromata,Computer,IT-Services",
            StringHelper.listToString(",", "Micromata", "Computer", "IT-Services"));
    assertEquals("Micromata", StringHelper.listToString(",", "Micromata"));
    assertEquals("(Micromata == ?) and (Computer == ?) and (IT-Services == ?)",
            StringHelper.listToExpressions(" and ", "(", " == ?)", "Micromata", "Computer", "IT-Services"));
    assertEquals("(Micromata == ?)", StringHelper.listToExpressions(" and ", "(", " == ?)", "Micromata"));

    assertEquals("a,b,c", StringHelper.listToString(",", "a", null, "b", "", "c"));

    assertEquals("1,2", StringHelper.listToString(",", new Object[]{1, 2}));
  }

  @Test
  public void sortAndUnique() {
    assertNull(StringHelper.sortAndUnique(null));
    compareStringArray(new String[]{}, StringHelper.sortAndUnique(new String[]{}));
    compareStringArray(new String[]{"hallo"}, StringHelper.sortAndUnique(new String[]{"hallo"}));
    compareStringArray(new String[]{"hallo"}, StringHelper.sortAndUnique(new String[]{"hallo", "hallo"}));
    compareStringArray(new String[]{"1", "2", "3"},
            StringHelper.sortAndUnique(new String[]{"1", "3", "2", "1", "3"}));
  }

  @Test
  public void isIn() {
    assertTrue(StringHelper.isIn("open", new String[]{"open", "close", "explore", "implore"}));
    assertTrue(StringHelper.isIn("close", new String[]{"open", "close", "explore", "implore"}));
    assertTrue(StringHelper.isIn("explore", new String[]{"open", "close", "explore", "implore"}));
    assertTrue(StringHelper.isIn("implore", new String[]{"open", "close", "explore", "implore"}));
    assertFalse(StringHelper.isIn("pen", new String[]{"open", "close", "explore", "implore"}));
    assertFalse(StringHelper.isIn(null, new String[]{"open", "close", "explore", "implore"}));
  }

  @Test
  public void endsWith() {
    assertFalse(StringHelper.endsWith(null));
    assertFalse(StringHelper.endsWith(null, ".gif"));
    assertFalse(StringHelper.endsWith("icon.png", ".gif"));
    assertTrue(StringHelper.endsWith("icon.gif", ".gif"));
    assertTrue(StringHelper.endsWith("icon.png", ".gif", ".png"));
    assertTrue(StringHelper.endsWith(".png", ".gif", ".png"));
  }

  @Test
  public void startsWith() {
    assertTrue(StringHelper.startsWith("Hurzel", "Hu"));
    assertFalse(StringHelper.startsWith(null, "Hu"));
    assertFalse(StringHelper.startsWith(null, (String) null));
    try {
      assertFalse(StringHelper.startsWith("Hurzel", (String) null));
      fail();
    } catch (final NullPointerException ex) {
    }
    assertFalse(StringHelper.startsWith("Hurzel", "foo"));
    assertTrue(StringHelper.startsWith("Hurzel", "Ha", "Hu"));
  }

  @Test
  public void format2DigitNumber() {
    assertEquals("00", StringHelper.format2DigitNumber(0));
    assertEquals("01", StringHelper.format2DigitNumber(1));
    assertEquals("09", StringHelper.format2DigitNumber(9));
    assertEquals("10", StringHelper.format2DigitNumber(10));
    assertEquals("23", StringHelper.format2DigitNumber(23));
    assertEquals("99", StringHelper.format2DigitNumber(99));
    assertEquals("100", StringHelper.format2DigitNumber(100));
  }

  @Test
  public void format3DigitNumber() {
    assertEquals("000", StringHelper.format3DigitNumber(0));
    assertEquals("001", StringHelper.format3DigitNumber(1));
    assertEquals("099", StringHelper.format3DigitNumber(99));
    assertEquals("999", StringHelper.format3DigitNumber(999));
    assertEquals("1000", StringHelper.format3DigitNumber(1000));
  }

  @Test
  public void removeNonDigits() {
    assertEquals("", StringHelper.removeNonDigits(null));
    assertEquals("", StringHelper.removeNonDigits("a"));
    assertEquals("1", StringHelper.removeNonDigits("1"));
    assertEquals("495613167930", StringHelper.removeNonDigits("+49 561 / 316793 - 0"));
    assertEquals("056131679311", StringHelper.removeNonDigits("0561 / 316793-11"));
  }

  @Test
  public void removeNonDigitsAndNonASCIILetters() {
    assertEquals("", StringHelper.removeNonDigitsAndNonASCIILetters(null));
    assertEquals("", StringHelper.removeNonDigitsAndNonASCIILetters("."));
    assertEquals("e1", StringHelper.removeNonDigitsAndNonASCIILetters(".éeö1-.;:_'*`´ $%&/()=@"));
  }

  @Test
  public void abbreviate() {
    final int[] maxWidth = new int[]{5, 5, 100};
    String str = StringHelper.abbreviate(new String[]{"1", "Hello", "ProjectForge"}, maxWidth, 22, ": ");
    assertEquals("1: Hello: ProjectForge", str);
    assertTrue(str.length() <= 22);
    str = StringHelper.abbreviate(new String[]{"11234567", "Hello, how are you?",
            "ProjectForge is the world fines Project management app."}, maxWidth, 22, ": ");
    assertTrue(str.length() == 22);
    assertEquals("11...: He...: Proje...", str);
    str = StringHelper.abbreviate(new String[]{null, "1", "ProjectForge is the world fines Project management app."},
            maxWidth, 22, ": ");
    assertTrue(str.length() == 22);
    assertEquals("1: ProjectForge is ...", str);
  }

  @Test
  public void getWildcardString() {
    assertEquals("", StringHelper.getWildcardString((String[]) null));
    assertEquals("", StringHelper.getWildcardString(""));
    assertEquals("", StringHelper.getWildcardString("", null, ""));
    assertEquals("", StringHelper.getWildcardString("hallo", null, "hallo"));
    assertEquals("", StringHelper.getWildcardString(null, "hallo", "hallo"));
    assertEquals("", StringHelper.getWildcardString(null, null, null));
    assertEquals("", StringHelper.getWildcardString("", "", ""));
    assertEquals("", StringHelper.getWildcardString("h", "h", ""));
    assertEquals("h", StringHelper.getWildcardString("h", "h", "h"));
    assertEquals("h", StringHelper.getWildcardString("hallo", "hurz", "house"));
    assertEquals("hallo", StringHelper.getWildcardString("hallo", "hallo", "hallo"));
    assertEquals("hallo", StringHelper.getWildcardString("hallo1", "hallo2", "hallo3"));
  }

  @Test
  public void checkPhoneNumberFormat() {
    assertTrue(StringHelper.checkPhoneNumberFormat(null));
    assertTrue(StringHelper.checkPhoneNumberFormat(""));
    assertTrue(StringHelper.checkPhoneNumberFormat(" "));
    assertTrue(StringHelper.checkPhoneNumberFormat("+49 561 316793-0"));
    assertFalse(StringHelper.checkPhoneNumberFormat("+490561 123456"), "+490561 123456 not allowed.");
    assertFalse(StringHelper.checkPhoneNumberFormat("+49 0561 123456"), "+49 0561 123456 not allowed.");
    assertFalse(StringHelper.checkPhoneNumberFormat("+49     0561 123456"), "+49 0561 123456 not allowed.");
    assertFalse(StringHelper.checkPhoneNumberFormat("0561 316793-0"), "Leading country code expected.");
    assertFalse(StringHelper.checkPhoneNumberFormat("+49 561 316793+0"), "+ is only allowed at first char");
  }

  @Test
  public void hideStringEnding() {
    assertEquals(null, StringHelper.hideStringEnding(null, 'x', 3));
    assertEquals("0170 12345xxx", StringHelper.hideStringEnding("0170 12345678", 'x', 3));
    assertEquals("0xxx", StringHelper.hideStringEnding("0170", 'x', 3));
    assertEquals("xxx", StringHelper.hideStringEnding("017", 'x', 3));
    assertEquals("xx", StringHelper.hideStringEnding("01", 'x', 3));
    assertEquals("x", StringHelper.hideStringEnding("0", 'x', 3));
    assertEquals("", StringHelper.hideStringEnding("", 'x', 3));
  }

  @Test
  public void splitToInts() {
    compareIntArray(new int[]{1, 111, 5, 11}, StringHelper.splitToInts("1.111.05.11", "."));
    compareIntArray(new int[]{1, 0, 5, 11}, StringHelper.splitToInts("1.null.05.11", "."));
    compareIntArray(new int[]{1, 111, 5, 11}, StringHelper.splitToInts("1, 111,05,11", ",", false));
    compareIntArray(new int[]{1, 5, 11}, StringHelper.splitToInts("1,null,05, ,,11", ",", false));
  }

  @Test
  public void splitAndTrim() {
    assertNull(StringHelper.splitAndTrim(null, ","));
    compareStringArray(new String[]{}, StringHelper.splitAndTrim("", ","));
    compareStringArray(new String[]{"a", "b"}, StringHelper.splitAndTrim("a, b", ","));
    compareStringArray(new String[]{"a", "b"}, StringHelper.splitAndTrim(",a,,, b,", ","));
    compareStringArray(new String[]{"a", "", "b c"}, StringHelper.splitAndTrim(",a, ,, b c,", ","));
  }

  @Test
  public void isNotBlank() {
    assertEquals(false, StringHelper.isNotBlank());
    assertEquals(false, StringHelper.isNotBlank((String[]) null));
    assertEquals(false, StringHelper.isNotBlank(null, ""));
    assertEquals(false, StringHelper.isNotBlank(null, "", " \t\n"));
    assertEquals(true, StringHelper.isNotBlank("a"));
    assertEquals(true, StringHelper.isNotBlank(null, "a", ""));
  }

  @Test
  public void compareTo() {
    assertEquals(0, StringHelper.compareTo(null, null));
    assertTrue(StringHelper.compareTo(null, "") < 0);
    assertTrue(StringHelper.compareTo(null, "Hurzel") < 0);
    assertTrue(StringHelper.compareTo("", null) > 0);
    assertEquals(0, StringHelper.compareTo("", ""));
    assertTrue(StringHelper.compareTo("Hurzel", null) > 0);
    assertTrue(StringHelper.compareTo("", "Test") < 0);
    assertTrue(StringHelper.compareTo("Anton", "Berta") < 0);
    assertEquals(0, StringHelper.compareTo("Anton", "Anton"));
    assertTrue(StringHelper.compareTo("Berta", "") > 0);
    assertTrue(StringHelper.compareTo("Berta", "Anton") > 0);
    assertEquals(0, StringHelper.compareTo("Anton", "Anton"));
  }

  @Test
  public void asHex() {
    assertEquals("", StringHelper.asHex(null));
    assertEquals("", StringHelper.asHex(new byte[]{}));
    assertEquals("00", StringHelper.asHex(new byte[]{0}));
    assertEquals("000a0f", StringHelper.asHex(new byte[]{0, 0x0a, 0x0f}));
    assertEquals("000aef", StringHelper.asHex(new byte[]{0, 0x0a, (byte) 0xef}));
  }

  @Test
  public void blank() {
    testBlank(true, (String[]) null);
    testBlank(true, null, null);
    testBlank(true, null, "");
    testBlank(true, " ", null);
    testBlank(false, ".");
    testBlank(false, null, ".");
    testBlank(false, null, ".", null);
  }

  @Test
  public void testSplitKeyValues() {
    Map<String, String> map = StringHelper.getKeyValues(null, "&");
    assertEquals(0, map.size());
    map = StringHelper.getKeyValues("name=horst", "&");
    assertEquals(1, map.size());
    assertEquals("horst", map.get("name"));
    map = StringHelper.getKeyValues("name=horst&param=value", "&");
    assertEquals(2, map.size());
    assertEquals("horst", map.get("name"));
    assertEquals("value", map.get("param"));
    map = StringHelper.getKeyValues("name=&param=value&empty=", "&");
    assertEquals(1, map.size());
    assertEquals("value", map.get("param"));
  }

  private void testBlank(final boolean expectedValue, final String... strs) {
    assertEquals(expectedValue, StringHelper.isBlank(strs));
    assertEquals(!expectedValue, StringHelper.isNotBlank(strs));
  }

  private void compareIntArray(final int[] a1, final int[] a2) {
    assertEquals(a1.length, a2.length);
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }
  }

  private void compareStringArray(final String[] a1, final String[] a2) {
    assertEquals(a1.length, a2.length);
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }
  }
}
