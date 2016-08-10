package org.projectforge.plugins.eed;

import java.util.Arrays;
import java.util.List;

import org.projectforge.export.AttrColumnDescription;

public final class ExtendEmployeeDataConstants
{
  /**
   * If you change values here, you have to change EmployeeBillingExcelRow accordingly.
   */
  public final static List<AttrColumnDescription> ATTR_FIELDS_TO_EDIT = Arrays.asList(
      new AttrColumnDescription("mobilecheck", "mobilecheck", "fibu.employee.mobilecheck.title"),
      new AttrColumnDescription("ebikeleasing", "ebikeleasing", "fibu.employee.ebikeleasing.title"),
      new AttrColumnDescription("daycarecenter", "daycarecenter", "fibu.employee.daycarecenter.title")
  );
}
