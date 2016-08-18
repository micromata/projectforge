package org.projectforge.plugins.eed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.projectforge.export.AttrColumnDescription;

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
