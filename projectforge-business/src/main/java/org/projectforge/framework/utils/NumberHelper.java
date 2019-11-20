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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class NumberHelper {
  public static final String ALLOWED_PHONE_NUMBER_CHARS = "+-/().";

  public static final int KILO_BYTES = 1024;

  public static final BigDecimal KB_BD = new BigDecimal(KILO_BYTES);

  public static final int MEGA_BYTES = KILO_BYTES * 1024;

  public static final BigDecimal MB_BD = new BigDecimal(MEGA_BYTES);

  public static final int GIGA_BYTES = MEGA_BYTES * 1024;

  public static final BigDecimal GB_BD = new BigDecimal(GIGA_BYTES);

  public static final BigDecimal TWENTY = new BigDecimal(20);

  public static final BigDecimal HUNDRED = new BigDecimal(100);

  public static final BigDecimal THOUSAND = new BigDecimal(1000);

  public static final BigDecimal THREE_THOUSAND_SIX_HUNDRED = new BigDecimal(3600);

  public static final BigDecimal MINUS_TWENTY = new BigDecimal(-20);

  public static final BigDecimal MINUS_HUNDRED = new BigDecimal(-100);

  public static final BigDecimal BILLION = new BigDecimal(1000000000);

  private static final Logger log = LoggerFactory.getLogger(NumberHelper.class);

  public static NumberFormat getCurrencyFormat(final Locale locale) {
    return getNumberFraction2Format(locale);
  }

  public static NumberFormat getNumberFraction2Format(final Locale locale) {
    final NumberFormat format = NumberFormat.getNumberInstance(locale);
    format.setMaximumFractionDigits(2);
    format.setMinimumFractionDigits(2);
    return format;
  }

  public static NumberFormat getNumberFractionFormat(final Locale locale, final int fractionDigits) {
    final NumberFormat format = NumberFormat.getNumberInstance(locale);
    format.setMaximumFractionDigits(fractionDigits);
    format.setMinimumFractionDigits(fractionDigits);
    return format;
  }

  /**
   * Pretty output of bytes, "1023 bytes", "1.1 kb", "523 kb", "1.7 Mb", "143 Gb" etc.
   *
   * @param bytes
   * @return
   */
  public static String formatBytes(final long bytes) {
    if (bytes < KILO_BYTES) {
      return String.valueOf(bytes) + " bytes";
    }
    if (bytes < MEGA_BYTES) {
      BigDecimal no = new BigDecimal(bytes).divide(KB_BD, 1, BigDecimal.ROUND_HALF_UP);
      if (no.longValue() >= 100) {
        no = no.setScale(0, BigDecimal.ROUND_HALF_UP);
      }
      return NumberFormat.getInstance(ThreadLocalUserContext.getLocale()).format(no) + " kb";
    }
    if (bytes < GIGA_BYTES) {
      BigDecimal no = new BigDecimal(bytes).divide(MB_BD, 1, BigDecimal.ROUND_HALF_UP);
      if (no.longValue() >= 100) {
        no = no.setScale(0, BigDecimal.ROUND_HALF_UP);
      }
      return NumberFormat.getInstance(ThreadLocalUserContext.getLocale()).format(no) + " Mb";
    }
    BigDecimal no = new BigDecimal(bytes).divide(GB_BD, 1, BigDecimal.ROUND_HALF_UP);
    if (no.longValue() >= 100) {
      no = no.setScale(0, BigDecimal.ROUND_HALF_UP);
    }
    return NumberFormat.getInstance(ThreadLocalUserContext.getLocale()).format(no) + " Gb";
  }

  /**
   * @param value
   * @return true, if value is not null and greater zero.
   */
  public static boolean greaterZero(final Integer value) {
    return value != null && value > 0;
  }

  /**
   * @param value
   * @return true, if value is not null and greater zero.
   */
  public static boolean greaterZero(final Long value) {
    return value != null && value.intValue() > 0;
  }

  public static boolean isZeroOrNull(final Integer value) {
    return (value == null || value == 0);
  }

  public static boolean isGreaterZero(final BigDecimal value) {
    return (value != null && value.compareTo(BigDecimal.ZERO) > 0);
  }

  /**
   * @param value
   * @return true, if the given value is not null and not zero.
   */
  public static boolean isNotZero(final Integer value) {
    return !isZeroOrNull(value);
  }

  /**
   * Parses the given string as integer value.
   *
   * @param value The string representation of the integer value to parse.
   * @return Integer value or null if an empty string was given or a syntax error occurs.
   */
  public static Integer parseInteger(String value) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    Integer result = null;
    try {
      result = new Integer(value);
    } catch (final NumberFormatException ex) {
      log.warn("Can't parse integer: '" + value + "'.");
    }
    return result;
  }

  /**
   * Parses the given string as short value.
   *
   * @param value The string representation of the short value to parse.
   * @return Short value or null if an empty string was given or a syntax error occurs.
   */
  public static Short parseShort(String value) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    Short result = null;
    try {
      result = new Short(value);
    } catch (final NumberFormatException ex) {
      log.debug(ex.getMessage(), ex);
    }
    return result;
  }

  /**
   * Catches any NumberFormatException and returns 0, otherwise the long value represented by the given value is returned.
   */
  public static Long parseLong(String value) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    Long result = null;
    try {
      result = new Long(value);
    } catch (final NumberFormatException ex) {
      log.debug(ex.getMessage(), ex);
    }
    return result;
  }

  /**
   *
   */
  public static BigDecimal parseBigDecimal(String value) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    BigDecimal result = null;
    try {
      if (value.indexOf(',') > 0) {
        // Replace the german decimal character by '.':
        value = value.replace(',', '.');
      }
      result = new BigDecimal(value);
    } catch (final NumberFormatException ex) {
      log.debug(ex.getMessage(), ex);
    }
    return result;
  }

  /**
   *
   */
  public static BigDecimal parseCurrency(String value, final Locale locale) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    final NumberFormat format = getCurrencyFormat(locale);
    BigDecimal result = null;
    try {
      final Number number = format.parse(value);
      if (number != null) {
        result = new BigDecimal(number.toString());
        result = result.setScale(2, BigDecimal.ROUND_HALF_UP);
      }
    } catch (final ParseException ex) {
      log.debug(ex.getMessage(), ex);
    }
    return result;
  }

  /**
   * @param v1 null is supported.
   * @param v2 null is supported.
   * @return
   */
  public static BigDecimal add(final BigDecimal v1, final BigDecimal v2) {
    if (v1 == null) {
      if (v2 == null) {
        return BigDecimal.ZERO;
      } else {
        return v2;
      }
    } else {
      if (v2 == null) {
        return v1;
      } else {
        return v1.add(v2);
      }
    }
  }

  /**
   * Returns the given integer value as String representation.
   *
   * @param value The integer value to convert.
   * @return The String representation or empty String, if value is null.
   */
  public static String getAsString(final Number value) {
    if (value == null) {
      return "";
    } else {
      return String.valueOf(value);
    }
  }

  /**
   * Returns the given number value as String representation.
   *
   * @param value  The number value to convert.
   * @param format The format to use.
   * @return The String representation or empty String, if value is null.
   */
  public static String getAsString(final Number value, final NumberFormat format) {
    if (value == null) {
      return "";
    } else {
      return format.format(value);
    }
  }

  /**
   * @see ThreadLocalUserContext#getLocale()
   */
  public static String formatFraction2(final Number value) {
    final Locale locale = ThreadLocalUserContext.getLocale();
    final NumberFormat format = getNumberFraction2Format(locale);
    return format.format(value);
  }

  /**
   * Uses the default country phone prefix from the configuration.
   *
   * @see #extractPhonenumber(String, String)
   */
  public static String extractPhonenumber(final String str) {
    final String defaultCountryPhonePrefix = Configuration.getInstance().getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX);

    return extractPhonenumber(str, defaultCountryPhonePrefix);
  }

  /**
   * Extracts the phone number of the given string. All characters of the set "+-/()." and white spaces will be deleted and +## will be
   * replaced by 00##. Example: +49 561 / 316793-0 -> 00495613167930 <br/>
   * Ignores any characters after the first occurence of ':' or any letter.
   *
   * @param str
   * @param countryPrefix If country prefix is given, for all numbers beginning with the country prefix the country prefix will be replaced
   *                      by 0. Example: ("+49 561 / 316793-0", "+49") -> 05613167930; ("+39 123456", "+49") -> 0039123456.
   * @return
   */
  public static String extractPhonenumber(String str, final String countryPrefix) {
    if (str == null) {
      return null;
    }
    str = str.trim();
    str = str.replaceAll("\\p{C}", ""); // Replace UTF controls chars, such as UTF-202C or UTF-202D (from Apple contacts app).
    final StringBuilder buf = new StringBuilder();
    if (StringUtils.isNotEmpty(countryPrefix) && str.startsWith(countryPrefix)) {
      buf.append('0');
      str = str.substring(countryPrefix.length());
    } else if (str.length() > 3
            && str.charAt(0) == '+'
            && Character.isDigit(str.charAt(1))
            && Character.isDigit(str.charAt(2))) {
      buf.append("00");
      buf.append(str.charAt(1));
      buf.append(str.charAt(2));
      str = str.substring(3);
    }
    for (int i = 0; i < str.length(); i++) {
      final char ch = str.charAt(i);
      if (Character.isDigit(str.charAt(i))) {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  public static boolean matchesPhoneNumber(String str) {
    return str.matches("^\\+?[0-9/\\-\\(\\)\\s]+$") && str.matches(".*\\d.*");
  }

  /**
   * Compares two given BigDecimals. They are equal if the value is equal independent of the scale (5.70 is equals to 5.7 and null is equals
   * null, but null is not equals to 0).
   *
   * @param value1
   * @param value2
   * @return
   * @see BigDecimal#compareTo(BigDecimal)
   */
  public static boolean isEqual(final BigDecimal value1, final BigDecimal value2) {
    if (value1 == null) {
      return (value2 == null) ? true : false;
    }
    if (value2 == null) {
      return false;
    }
    return value1.compareTo(value2) == 0;
  }

  /**
   * @param value
   * @return true, if the given value is not null and not zero.
   */
  public static boolean isNotZero(final BigDecimal value) {
    return !isZeroOrNull(value);
  }

  public static boolean isZeroOrNull(final BigDecimal value) {
    return (value == null || value.compareTo(BigDecimal.ZERO) == 0);
  }

  /**
   * Compares two given Integers using compareTo method.
   *
   * @param value1
   * @param value
   * @return
   * @see Integer#compareTo(Integer)
   */
  public static boolean isEqual(final Integer value1, final Integer value) {
    if (value1 == null) {
      return (value == null) ? true : false;
    }
    if (value == null) {
      return false;
    }
    return value1.compareTo(value) == 0;
  }

  /**
   * Splits string representation of the given number into digits. Examples:<br/>
   * NumberHelper.splitToInts(11110511, 1, 3, 2, 2) = {1, 111, 5, 11}<br/>
   * NumberHelper.splitToInts(10000511, 1, 3, 2, 2) = { 1, 0, 5, 11}<br/>
   * NumberHelper.splitToInts(511, 1, 3, 2, 2) = { 0, 0, 5, 11}
   *
   * @param value
   * @param split
   * @return
   */
  public static int[] splitToInts(final Number value, final int... split) {
    int numberOfDigits = 0;
    for (final int n : split) {
      numberOfDigits += n;
    }
    final String str = StringUtils.leftPad(String.valueOf(value.intValue()), numberOfDigits, '0');
    final int[] result = new int[split.length];
    int pos = 0;
    int i = 0;
    for (final int n : split) {
      result[i++] = parseInteger(str.substring(pos, pos + n));
      pos += n;
    }
    return result;
  }

  /**
   * If given string is an number (NumberUtils.isNumber(String)) then it will be converted to a plain string via BigDecimal.toPlainString().
   * Any exponent such as 1E7 will be avoided.
   *
   * @param str
   * @return Converted string if number, otherwise the origin string.
   * @see NumberUtils#isCreatable(String)
   * @see NumberUtils#createBigDecimal(String)
   * @see BigDecimal#toPlainString()
   */
  public static String toPlainString(final String str) {
    if (NumberUtils.isCreatable(str)) {
      final BigDecimal bd = NumberUtils.createBigDecimal(str);
      return bd.toPlainString();
    } else {
      return str;
    }
  }

  /**
   * Sets scale 0 for numbers greater 100, 1 for numbers greater 20 and 2 as default.
   *
   * @param number
   * @return
   */
  public static BigDecimal setDefaultScale(final BigDecimal number) {
    if (number == null) {
      return null;
    }
    if (number.compareTo(NumberHelper.HUNDRED) >= 0 || number.compareTo(NumberHelper.MINUS_HUNDRED) <= 0) {
      return number.setScale(0, BigDecimal.ROUND_HALF_UP);
    } else if (number.compareTo(NumberHelper.TWENTY) >= 0 || number.compareTo(NumberHelper.MINUS_TWENTY) <= 0) {
      return number.setScale(1, BigDecimal.ROUND_HALF_UP);
    }
    return number.setScale(2, BigDecimal.ROUND_HALF_UP);
  }

  /**
   * Generates secure random bytes of the given length and return base 64 encoded bytes as url safe String. This is not the length of the
   * resulting string!
   *
   * @param numberOfBytes
   * @return
   */
  public static String getSecureRandomUrlSaveString(final int numberOfBytes) {
    final SecureRandom random = new SecureRandom();
    final byte[] bytes = new byte[numberOfBytes];
    random.nextBytes(bytes);
    return Base64.encodeBase64URLSafeString(bytes);
  }

  /**
   * Generates secure random bytes of the given length and return base 64 encoded bytes as url safe String. This is not the length of the
   * resulting string!
   *
   * @param numberOfBytes
   * @return
   */
  public static String getSecureRandomBase64String(final int numberOfBytes) {
    final SecureRandom random = new SecureRandom();
    final byte[] bytes = new byte[numberOfBytes];
    random.nextBytes(bytes);
    return org.apache.commons.codec.binary.StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false));
  }

  public static boolean isIn(final int value, final int... numbers) {
    if (numbers == null) {
      return false;
    }
    for (final int number : numbers) {
      if (value == number) {
        return true;
      }
    }
    return false;
  }
}
