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

package org.projectforge.plugins.eed;

import org.projectforge.export.AttrColumnDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * If you change enum entries here, you have to change EmployeeBillingExcelRow accordingly.
 */
public enum ExtendEmployeeDataEnum
{

  //  MOBILECONTRACT("fibu.employee.mobilecontract.title",
  //      Collections.singletonList(new AttrColumnDescription("mobilecontract", "mobilecontract", "fibu.employee.mobilecontract.title"))),
  //
  //  MOBILECHECK("fibu.employee.mobilecheck.title",
  //      Collections.singletonList(new AttrColumnDescription("mobilecheck", "mobilecheck", "fibu.employee.mobilecheck.title"))),

  COSTTRAVEL("fibu.employee.costtravel.title",
      Collections.singletonList(new AttrColumnDescription("costtravel", "costtravel", "fibu.employee.costtravel.amount"))),

  EXPENSES("fibu.employee.expenses.title",
      Collections.singletonList(new AttrColumnDescription("expenses", "expenses", "fibu.employee.expenses.amount"))),

  OVERTIME("fibu.employee.overtime.title",
      Collections.singletonList(new AttrColumnDescription("overtime", "overtime", "fibu.employee.overtime.amount"))),

  BONUS("fibu.employee.bonus.title",
      Collections.singletonList(new AttrColumnDescription("bonus", "bonus", "fibu.employee.bonus.amount"))),

  SPECIALPAYMENT("fibu.employee.specialpayment.title",
      Collections.singletonList(new AttrColumnDescription("specialpayment", "specialpayment", "fibu.employee.specialpayment.amount"))),

  TARGETAGREEMENTS("fibu.employee.targetagreements.title",
      Collections.singletonList(new AttrColumnDescription("targetagreements", "targetagreements", "fibu.employee.targetagreements.amount"))),

  COSTSHOP("fibu.employee.costshop.title",
      Arrays.asList(
          new AttrColumnDescription("costshop", "costshop", "fibu.employee.costshop.amount"),
          new AttrColumnDescription("costshop", "contents", "fibu.employee.costshop.contents"))),

  WEEKENDWORK("fibu.employee.weekendwork.title",
      Arrays.asList(
          new AttrColumnDescription("weekendwork", "workinghourssaturday", "fibu.employee.weekendwork.saturday"),
          new AttrColumnDescription("weekendwork", "workinghourssunday", "fibu.employee.weekendwork.sunday"),
          new AttrColumnDescription("weekendwork", "workinghoursholiday", "fibu.employee.weekendwork.holiday"))),

  OTHERS("fibu.employee.others.title",
      Collections.singletonList(new AttrColumnDescription("others", "others", "fibu.employee.others.remarks")));

  private final String i18nKeyDropDown;

  private final List<AttrColumnDescription> attrColumnDescriptions;

  ExtendEmployeeDataEnum(String i18nKeyDropDown, List<AttrColumnDescription> attrColumnDescriptions)
  {
    this.i18nKeyDropDown = i18nKeyDropDown;
    this.attrColumnDescriptions = attrColumnDescriptions;
  }

  public String getI18nKeyDropDown()
  {
    return i18nKeyDropDown;
  }

  public List<AttrColumnDescription> getAttrColumnDescriptions()
  {
    return attrColumnDescriptions;
  }

  public static List<AttrColumnDescription> getAllAttrColumnDescriptions()
  {
    List<AttrColumnDescription> resultList = new ArrayList<>();
    for (ExtendEmployeeDataEnum eede : values()) {
      resultList.addAll(eede.getAttrColumnDescriptions());
    }
    return resultList;
  }
}
