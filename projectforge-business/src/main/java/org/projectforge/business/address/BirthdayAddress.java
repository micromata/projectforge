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

package org.projectforge.business.address;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DateHolder;


public class BirthdayAddress implements Comparable<BirthdayAddress>, Serializable
{
  private static final long serialVersionUID = -611024181799550736L;

  private final String dateOfYear;

  private final String compareString;

  private final int month;

  private final int dayOfMonth;

  private boolean isFavorite;

  private int age = -1;

  AddressDO address;

  public BirthdayAddress(final AddressDO address)
  {
    this.address = address;
    if (address.getBirthday() == null) {
      throw new UnsupportedOperationException("Birthday not given!");
    }
    final DateHolder day = new DateHolder(address.getBirthday());
    month = day.getMonth();
    dayOfMonth = day.getDayOfMonth();
    dateOfYear = getDateOfYear(month, dayOfMonth);
    compareString = dateOfYear + " " + address.getName() + ", " + address.getFirstName();
  }

  @Override
  public boolean equals(final Object obj)
  {
    return compareString.equals(((BirthdayAddress) obj).compareString);
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.compareString);
    return hcb.toHashCode();
  }

  @Override
  public int compareTo(final BirthdayAddress o)
  {
    return this.compareString.compareTo(o.compareString);
  }

  /**
   * Ignores the year!
   * @see #getDateOfYear(Date)
   */
  public boolean isSameDay(final Date date)
  {
    return this.dateOfYear.equals(getDateOfYear(date));
  }

  /** Sets and gets the age of the person at the given date. */
  public int setAge(final Date date)
  {
    final DateHolder dh = new DateHolder(date);
    final DateHolder birthday = new DateHolder(address.getBirthday());
    age = dh.getYear() - birthday.getYear();
    return age;
  }

  /** Gets the age of the person at the date of the last call of getAge(Date). */
  public int getAge()
  {
    return age;
  }

  public AddressDO getAddress()
  {
    return address;
  }

  /**
   * Format {mmdd} (month, day of month)
   */
  public String getDateOfYear()
  {
    return dateOfYear;
  }

  /**
   * Format {mmdd name, firstname} (month, day of month, ...)
   */
  public String getCompareString()
  {
    return compareString;
  }

  public boolean isFavorite()
  {
    return isFavorite;
  }

  public void setFavorite(final boolean isFavorite)
  {
    this.isFavorite = isFavorite;
  }

  /**
   * 0 - January, 1 - February...
   */
  public int getMonth()
  {
    return month;
  }

  /**
   * Java calendar style.
   */
  public int getDayOfMonth()
  {
    return dayOfMonth;
  }

  @Override
  public String toString()
  {
    return compareString;
  }

  /**
   * Gets the date of year as string (without year) in format {mmdd}.
   * @param date
   */
  public static String getDateOfYear(final Date date)
  {
    if (date == null) {
      throw new UnsupportedOperationException("Date not given!");
    }
    final DateHolder dh = new DateHolder(date);
    return getDateOfYear(dh.getMonth(), dh.getDayOfMonth());
  }

  public static String getDateOfYear(final int month, final int dayOfMonth)
  {
    return StringHelper.format2DigitNumber(month) + StringHelper.format2DigitNumber(dayOfMonth);
  }
}
