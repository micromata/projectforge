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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.sql.Timestamp;
import java.util.*;

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class StringHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StringHelper.class);

  /**
   * Usage: final StringBuffer buf = new StringBuffer();<br/>
   * boolean first = true;<br/>
   * if (...) {<br/>
   * first = StringHelper.append(buf, first, "Hurzel", ", ");<br/>
   * <br/>
   * first = StringBuffer.append(buf, first, myString, ", ");<br/>
   *
   * @param buf
   * @param first
   * @param str String to append. If null, nothing will be done and first will be returned.
   * @param delimiter
   * @return true if str is not empty and appended to buffer, otherwise first will be returned.
   */
  public static boolean append(final StringBuffer buf, final boolean first, final String str, final String delimiter)
  {
    if (StringUtils.isEmpty(str) == true) {
      return first;
    }
    if (first == false) {
      buf.append(delimiter);
    }
    buf.append(str);
    return false;
  }

  /**
   * Usage: final StringBuffer buf = new StringBuffer();<br/>
   * boolean first = true;<br/>
   * if (...) {<br/>
   * first = StringHelper.append(buf, first, "Hurzel", ", ");<br/>
   * <br/>
   * first = StringBuffer.append(buf, first, myString, ", ");<br/>
   *
   * @param sb
   * @param first
   * @param str String to append. If null, nothing will be done and first will be returned.
   * @param delimiter
   * @return true if str is not empty and appended to buffer, otherwise first will be returned.
   */
  public static boolean append(final StringBuilder sb, final boolean first, final String str, final String delimiter)
  {
    if (StringUtils.isEmpty(str) == true) {
      return first;
    }
    if (first == false) {
      sb.append(delimiter);
    }
    sb.append(str);
    return false;
  }

  public static int compareTo(final String s1, final String s2)
  {
    if (s1 == null) {
      if (s2 == null) {
        return 0;
      } else {
        return -1;
      }
    }
    if (s2 == null) {
      return +1;
    }
    return s1.compareTo(s2);
  }

  public static boolean isIn(final String string, final String... fields)
  {
    if (StringUtils.isEmpty(string) == true) {
      return false;
    }
    for (final String field : fields) {
      if (string.equals(field) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * Nullpointer save version of String.endsWith.
   *
   * @return True, if the given string ends with one of the given suffixes, otherwise false.
   * @see String#endsWith(String)
   */
  public static boolean endsWith(final String str, final String... suffixes)
  {
    if (str == null || suffixes == null) {
      return false;
    }
    for (final String suffix : suffixes) {
      if (str.endsWith(suffix) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * Nullpointer save version of String.startsWith.
   *
   * @return True, if the given string starts with one of the given prefixes, otherwise false.
   * @see String#startsWith(String)
   */
  public static boolean startsWith(final String str, final String... prefixes)
  {
    if (str == null || prefixes == null) {
      return false;
    }
    for (final String prefix : prefixes) {
      if (str.startsWith(prefix) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort If true, the given list will be first sorted.
   * @return
   */
  public static String listToString(final List<String> list, final String delimiter, final boolean sort)
  {
    if (sort == true) {
      Collections.sort(list);
    }
    return colToString(list, delimiter);
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort If true, the given list will be first sorted.
   * @return
   */
  public static String colToString(final Collection<String> col, final String delimiter)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final String item : col) {
      first = append(buf, first, item, delimiter);
    }
    return buf.toString();
  }

  /**
   * For example ["Micromata", "IT-Services", "Computer"] -> "Computer, IT-Services, Micromata".
   *
   * @param list List of input strings.
   * @param delimiter The delimiter of the single string in output string.
   * @param sort If true, the given list will be first sorted.
   * @return
   */
  public static String objectColToString(final Collection<?> col, final String delimiter)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final Object item : col) {
      final String str = item != null ? item.toString() : "";
      first = append(buf, first, str, delimiter);
    }
    return buf.toString();
  }

  /**
   * @param delimiter
   * @param strings
   * @see #listToString(List, String, boolean)
   */
  public static String listToString(final String delimiter, final String... strings)
  {
    final StringBuffer buf = new StringBuffer();
    return listToString(buf, delimiter, strings);
  }

  /**
   * @param delimiter
   * @param strings
   * @see #listToString(List, String, boolean)
   */
  public static String listToString(final StringBuffer buf, final String delimiter, final String... strings)
  {
    if (strings == null) {
      return null;
    } else if (strings.length == 0) {
      return "";
    }
    boolean first = true;
    for (final String s : strings) {
      if (s == null || s.length() == 0) {
        continue;
      }
      first = append(buf, first, s, delimiter);
    }
    return buf.toString();
  }

  /**
   * @param delimiter
   * @param strings
   * @see #listToString(List, String, boolean)
   */
  public static String listToString(final String delimiter, final Object... oa)
  {
    if (oa == null) {
      return null;
    } else if (oa.length == 0) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final Object o : oa) {
      if (o == null) {
        continue;
      }
      final String s = o.toString();
      if (s == null || s.length() == 0) {
        continue;
      }
      first = append(buf, first, s, delimiter);
    }
    return buf.toString();
  }

  /**
   * @param delimiter
   * @param strings
   * @see #listToString(List, String, boolean)
   */
  public static String doublesToString(final String delimiter, final double... oa)
  {
    if (oa == null) {
      return null;
    } else if (oa.length == 0) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final Object o : oa) {
      if (o == null) {
        continue;
      }
      final String s = o.toString();
      if (s == null || s.length() == 0) {
        continue;
      }
      first = append(buf, first, s, delimiter);
    }
    return buf.toString();
  }

  /**
   *
   * @param delimiter
   * @param prefix will be prepended before every string.
   * @param suffix will be appended to every string.
   * @param strings
   * @return
   * @see #listToString(List, String, boolean)
   */
  public static String listToExpressions(final String delimiter, final String prefix, final String suffix,
      final String... strings)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final String s : strings) {
      append(buf, first, prefix, delimiter);
      if (first == true)
        first = false;
      buf.append(s).append(suffix);
    }
    return buf.toString();
  }

  public static String[] sortAndUnique(final String[] array)
  {
    if (array == null || array.length <= 1) {
      return array;
    }
    final Set<String> set = new TreeSet<String>();
    set.addAll(Arrays.asList(array));
    final String[] result = (set.toArray(new String[set.size()]));
    return result;
  }

  public static String timestampToSearchString(final Timestamp timestamp)
  {
    if (timestamp == null) {
      return "";
    }
    return timestamp.toString();
  }

  public static String dateToSearchString(final Date date)
  {
    if (date == null) {
      return "";
    }
    return date.toString();
  }

  /**
   * 0 -&gt; "00", 1 -&gt; "01", ..., 9 -&gt; "09", 10 -&gt; "10", 100 -&gt; "100" etc. Uses StringUtils.leftPad(str, 2,
   * '0');
   *
   * @param value
   * @return
   * @see StringUtils#leftPad(String, int, char)
   */
  public static String format2DigitNumber(final int value)
  {
    return StringUtils.leftPad(String.valueOf(value), 2, '0');
  }

  /**
   * 0 -&gt; "000", 1 -&gt; "001", ..., 9 -&gt; "009", 10 -&gt; "010", 100 -&gt; "0100", 1000 -&gt; "1000" etc. Uses
   * StringUtils.leftPad(str, 2, '0');
   *
   * @param value
   * @return
   * @see StringUtils#leftPad(String, int, char)
   */
  public static String format3DigitNumber(final int value)
  {
    return StringUtils.leftPad(String.valueOf(value), 3, '0');
  }

  /**
   * Remove all non digits from the given string and return the result. If null is given, "" is returned.
   *
   * @param str
   * @return
   */
  public static String removeNonDigits(final String str)
  {
    if (str == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      final char ch = str.charAt(i);
      if (ch >= '0' && ch <= '9') {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  public static String removeNonDigitsAndNonASCIILetters(final String str)
  {
    if (str == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      final char ch = str.charAt(i);
      if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch >= '0' && ch <= '9') {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  /**
   * Formats string array, each string with max with and separated by separator with a total width. See StringHelperTest
   * for documentation.
   *
   * @param strings
   * @param maxWidth
   * @param maxTotalLength
   * @param separator
   * @return
   */
  public static String abbreviate(final String[] strings, final int[] maxWidth, final int maxTotalLength,
      final String separator)
  {
    Validate.notNull(strings);
    Validate.notNull(maxWidth);
    Validate.isTrue(strings.length == maxWidth.length);
    int rest = maxTotalLength;
    final StringBuffer buf = new StringBuffer();
    final int separatorLength = separator.length();
    boolean output = false;
    for (int i = 0; i < strings.length; i++) {
      final String str = strings[i];
      if (StringUtils.isBlank(str) == true) {
        continue;
      }
      if (output == true) {
        buf.append(separator);
        rest -= separatorLength;
      } else {
        output = true;
      }
      if (rest <= 0) {
        break;
      }
      final int max = Math.min(maxWidth[i], rest);
      buf.append(StringUtils.abbreviate(str, max));
      rest -= Math.min(str.length(), max);
    }
    return buf.toString();
  }

  public static String removeWhitespaces(final String value)
  {
    if (value == null) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < value.length(); i++) {
      final char ch = value.charAt(i);
      if (Character.isWhitespace(ch) == false) {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  /**
   * Examples
   * <ul>
   * <li>null -&gt; ""</li>
   * <li>"Hello", "Hello ProjectForge", "Hello kitty" -&gt; "Hello"</li>
   * <li>"Hello", null, "Hello kitty" -&gt; ""</li>
   * </ul>
   *
   * @param strs
   * @return The wild card string that matches all given strings. If no matching found (null or empty strings given)
   *         then an empty string is returned.
   */
  public static String getWildcardString(final String... strs)
  {
    if (strs == null) {
      return "";
    }
    int maxLength = Integer.MAX_VALUE;
    for (final String str : strs) {
      if (str == null) {
        return "";
      } else if (str.length() < maxLength) {
        maxLength = str.length();
      }
    }
    for (int i = 0; i < maxLength; i++) {
      final char ch = strs[0].charAt(i);
      for (final String str : strs) {
        if (ch != str.charAt(i)) {
          if (i > 0) {
            return strs[0].substring(0, i);
          } else {
            return "";
          }
        }
      }
    }
    return strs[0].substring(0, maxLength);
  }

  /**
   * Valid characters are ''+'' as first char, ''-'', ''/'' and spaces. The leading country code is mandatory, e. g.:
   * +49 561 316793-0
   */
  public static boolean checkPhoneNumberFormat(final String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return true;
    }
    if (StringUtils.containsOnly(value, "+1234567890 -/") == false
        || value.startsWith("+") == false
        || value.length() < 2
        || Character.isDigit(value.charAt(1)) == false
        || value.indexOf('+', 1) != -1) {
      return false;
    }
    final String str = removeWhitespaces(value);
    if (str.startsWith("+49") && str.charAt(3) == '0') {
      // +49 0561 123456 is not allowed
      return false;
    }
    return true;
  }

  /**
   * Hides the last numberOfCharacters chars of the given string by replacing them with ch.<br/>
   * Examples:
   * <ul>
   * <li>hideStringEnding("0170 1234568", 'x', 3) -> "0170 12345xxx"</li>
   * <li>StringHelper.hideStringEnding("01", 'x', 3) -> "xx"</li>
   * <li>StringHelper.hideStringEnding(null, 'x', 3) -> "null</li>
   * </ul>
   *
   * @param str Original string.
   * @param ch Replace character.
   * @param numberOfCharacters
   * @return
   */
  public static String hideStringEnding(final String str, final char ch, final int numberOfCharacters)
  {
    if (str == null) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    final int toPos = str.length() - numberOfCharacters;
    for (int i = 0; i < str.length(); i++) {
      if (i < toPos) {
        buf.append(str.charAt(i));
      } else {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  public static Integer[] splitToIntegers(final String str, final String delim)
  {
    if (str == null) {
      return null;
    }
    final StringTokenizer tokenizer = new StringTokenizer(str, delim);
    final Integer[] result = new Integer[tokenizer.countTokens()];
    int i = 0;
    while (tokenizer.hasMoreTokens() == true) {
      final String token = tokenizer.nextToken();
      final Integer value = IntegerHelper.parseInteger(token);
      result[i++] = value;
    }
    return result;
  }

  public static int[] splitToInts(final String str, final String delim)
  {
    final StringTokenizer tokenizer = new StringTokenizer(str, delim);
    final int[] result = new int[tokenizer.countTokens()];
    int i = 0;
    while (tokenizer.hasMoreTokens() == true) {
      final String token = tokenizer.nextToken();
      final Integer value = IntegerHelper.parseInteger(token);
      result[i++] = value != null ? value : 0;
    }
    return result;
  }

  /**
   * @param str
   * @param delim
   * @param ignoreEmptyItems If true then "1, ,2" returns [1,0,2], otherwise [1,2] is returned.
   * @return
   */
  public static int[] splitToInts(final String str, final String delim, final boolean ignoreEmptyItems)
  {
    if (ignoreEmptyItems == true) {
      return splitToInts(str, delim);
    }
    final StringTokenizer tokenizer = new StringTokenizer(str, delim);
    final List<Integer> list = new ArrayList<Integer>(tokenizer.countTokens());
    while (tokenizer.hasMoreTokens() == true) {
      final String token = tokenizer.nextToken();
      final Integer value = IntegerHelper.parseInteger(token);
      if (value != null) {
        list.add(value);
      }
    }
    final int[] result = new int[list.size()];
    int i = 0;
    for (final Integer number : list) {
      result[i++] = number;
    }
    return result;
  }

  /**
   * Trims all string of the resulting array.
   *
   * @param str
   * @param separatorChars
   * @return
   * @see StringUtils#split(String, String)
   */
  public static String[] splitAndTrim(final String str, final String separatorChars)
  {
    if (str == null) {
      return null;
    }
    final String[] sa = StringUtils.split(str, separatorChars);
    final String[] result = new String[sa.length];
    for (int i = 0; i < sa.length; i++) {
      result[i] = sa[i].trim();
    }
    return result;
  }

  /**
   * Calls !{@link #isBlank(String...)}.
   *
   * @param strs
   * @return true if one of the given strings is not blank, otherwise false.
   * @see StringUtils#isNotBlank(String)
   */
  public static boolean isNotBlank(final String... strs)
  {
    return isBlank(strs) == false;
  }

  /**
   * Calls {@link StringUtils#isBlank(String)} for each of the given strings.
   *
   * @param strs
   * @return true if one of the given strings is not blank, otherwise false.
   * @see #isNotBlank(String...)
   */
  public static boolean isBlank(final String... strs)
  {
    if (strs == null) {
      return true;
    }
    for (final String s : strs) {
      if (StringUtils.isNotBlank(s) == true) {
        return false;
      }
    }
    return true;
  }

  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  public static String asHex(final byte[] buf)
  {
    if (buf == null || buf.length == 0) {
      return "";
    }
    final char[] chars = new char[2 * buf.length];
    for (int i = 0; i < buf.length; ++i) {
      chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
      chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
    }
    return new String(chars);
  }

  /**
   * @param keyValues e. g. "name=Horst,street=Baker street"
   * @param delimiter
   * @return
   */
  public static Map<String, String> getKeyValues(final String keyValues, final String delimiter)
  {
    final Map<String, String> map = new HashMap<String, String>();
    if (keyValues == null) {
      return map;
    }
    final StringTokenizer tokenizer = new StringTokenizer(keyValues, delimiter);
    while (tokenizer.hasMoreTokens() == true) {
      final String token = tokenizer.nextToken();
      final int pos = token.indexOf('=');
      if (pos <= 0) {
        log.error(
            "Bad request, decrypted parameter is not from type key=value: " + token + ". String was: " + keyValues);
        return null;
      }
      final String key = token.substring(0, pos);
      String value;
      if (pos == token.length() - 1) {
        continue;
      }
      value = token.substring(pos + 1);
      map.put(key, value);
    }
    return map;
  }
}
