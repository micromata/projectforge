package org.projectforge.plugins.eed.service;

import java.util.List;

import org.projectforge.plugins.eed.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.EmployeeConfigurationDao;
import org.projectforge.plugins.eed.EmployeeConfigurationTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeConfigurationService
{
  @Autowired
  private EmployeeConfigurationDao employeeConfigurationDao;

  /**
   * There is just one row in the employee configuration table. If it does not exist, create it once.
   *
   * @return The EmployeeConfigurationDO.
   */
  public EmployeeConfigurationDO getTheDO()
  {
    // TODO CT: check rights?
    final List<EmployeeConfigurationDO> employeeConfigurationDOs = employeeConfigurationDao.internalLoadAll();

    if (employeeConfigurationDOs.isEmpty()) {
      final EmployeeConfigurationDO employeeConfigurationDO = employeeConfigurationDao.newInstance();
      employeeConfigurationDao.save(employeeConfigurationDO);
      return employeeConfigurationDO;
    } else if (employeeConfigurationDOs.size() != 1) {
      throw new RuntimeException("Expected One employeeConfigurationDO, but found " + employeeConfigurationDOs.size());
    }

    return employeeConfigurationDOs.get(0);
  }

  public EmployeeConfigurationDao getDao()
  {
    return employeeConfigurationDao;
  }

  public EmployeeConfigurationTimedDO addNewTimeAttributeRow(final EmployeeConfigurationDO employeeConfiguration, final String groupName)
  {
    final EmployeeConfigurationTimedDO row = new EmployeeConfigurationTimedDO();
    row.setGroupName(groupName);
    employeeConfiguration.addTimeableAttribute(row);
    return row;
  }

}
