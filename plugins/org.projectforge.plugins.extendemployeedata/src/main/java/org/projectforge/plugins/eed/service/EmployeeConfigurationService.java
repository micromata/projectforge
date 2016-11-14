package org.projectforge.plugins.eed.service;

import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.projectforge.plugins.eed.model.EmployeeConfigurationTimedDO;

public interface EmployeeConfigurationService
    extends IPersistenceService<EmployeeConfigurationDO>, IDao<EmployeeConfigurationDO>
{
  public static final String STAFFNR_COLUMN_NAME_ATTR = "staffnrcolumnname";

  public static final String SALARY_COLUMN_NAME_ATTR = "salarycolumnname";

  public static final String REMARK_COLUMN_NAME_ATTR = "remarkcolumnname";

  EmployeeConfigurationTimedDO

  addNewTimeAttributeRow(final EmployeeConfigurationDO employeeConfiguration,
      final String groupName);

  Integer getSingleEmployeeConfigurationDOId();

  EmployeeConfigurationDO getSingleEmployeeConfigurationDO();
}
