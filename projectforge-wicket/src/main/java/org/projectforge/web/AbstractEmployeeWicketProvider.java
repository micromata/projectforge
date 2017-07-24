package org.projectforge.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;

/**
 * Created by blumenstein on 13.12.16.
 */
public abstract class AbstractEmployeeWicketProvider extends ChoiceProvider<EmployeeDO>
{
  protected List<EmployeeDO> sortedEmployees;

  protected transient EmployeeService employeeService;

  protected int pageSize = 20;

  public AbstractEmployeeWicketProvider(final EmployeeService employeeService)
  {
    this.employeeService = employeeService;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public AbstractEmployeeWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  public List<EmployeeDO> getSortedEmployees()
  {
    return sortedEmployees;
  }

  @Override
  public String getDisplayValue(final EmployeeDO choice)
  {
    return choice.getUser().getFullname();
  }

  @Override
  public String getIdValue(final EmployeeDO choice)
  {
    return String.valueOf(choice.getPk());
  }

  @Override
  public Collection<EmployeeDO> toChoices(final Collection<String> ids)
  {
    final List<EmployeeDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer employeeId = NumberHelper.parseInteger(str);
      if (employeeId == null) {
        continue;
      }
      EmployeeDO employee = employeeService.selectByPkDetached(employeeId);
      if (employee != null) {
        list.add(employee);
      }
    }
    return list;
  }

}
