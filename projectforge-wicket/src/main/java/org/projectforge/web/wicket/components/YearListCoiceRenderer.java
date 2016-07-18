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

package org.projectforge.web.wicket.components;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

/**
 * Helper class for rendering combo boxes: {"2007-2009"; "2009"; "2008"; "2007"} or {"2009"; "2008", "2007"}
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class YearListCoiceRenderer implements IChoiceRenderer<Integer>
{
  private static final long serialVersionUID = 3352647109199906373L;

  static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

  private List<Integer> years;

  private int minYear = Integer.MAX_VALUE;

  private int maxYear = 0;

  /**
   * @param years List of years, -1 represents min year until max year, e. g. "2007-2009" If no year is given then the current year is
   *                assumed as min and max year.
   */
  public YearListCoiceRenderer(List<Integer> years)
  {
    this.years = years;
    init();
  }

  /**
   * @param years
   * @param prependYearRange If true then '-1' is prepended.
   * @see #YearListCoiceRenderer(List)
   */
  public YearListCoiceRenderer(int[] years, boolean prependYearRange)
  {
    if (years != null && years.length > 1 && prependYearRange == true) {
      this.years = new ArrayList<Integer>(years.length + 1); // length + 1 enough for adding "-1".
      this.years.add(-1);
    } else {
      this.years = new ArrayList<Integer>(years.length);
    }
    for (int year : years) {
      this.years.add(year);
    }
    init();
  }

  private void init()
  {
    for (Integer year : years) {
      if (year < 0)
        continue;
      if (year < minYear)
        minYear = year;
      if (year > maxYear)
        maxYear = year;
    }
    if (minYear == Integer.MAX_VALUE)
      minYear = CURRENT_YEAR;
    if (maxYear == 0)
      maxYear = CURRENT_YEAR;
  }

  public List<Integer> getYears()
  {
    return years;
  }

  /**
   * Please note: This method does not check wether the given object is an entry of the year list or not.
   * @return given integer as String or "[minYear]-[maxYear]" if value is -1.
   * @see org.apache.wicket.markup.html.form.IChoiceRenderer#getDisplayValue(java.lang.Object)
   */
  public Object getDisplayValue(Integer object)
  {
    if (object < 0) {
      if (minYear == maxYear)
        return String.valueOf(minYear);
      else return String.valueOf(minYear) + "-" + maxYear;
    }
    return object.toString();
  }

  public String getIdValue(Integer object, int index)
  {
    return object.toString();
  }

}
