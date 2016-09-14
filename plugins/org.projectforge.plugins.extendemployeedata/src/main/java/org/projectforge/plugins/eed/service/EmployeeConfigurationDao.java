package org.projectforge.plugins.eed.service;

import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.plugins.eed.model.EmployeeConfigurationDO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class EmployeeConfigurationDao extends BaseDao<EmployeeConfigurationDO>
{
  protected EmployeeConfigurationDao()
  {
    super(EmployeeConfigurationDO.class);
    userRightId = UserRightId.HR_EMPLOYEE_SALARY; // this is used for right check from BaseDao::hasAccess which is used e.g. in BaseDao::checkLoggedInUserSelectAccess
  }

  @Override
  public EmployeeConfigurationDO newInstance()
  {
    return new EmployeeConfigurationDO();
  }
}
