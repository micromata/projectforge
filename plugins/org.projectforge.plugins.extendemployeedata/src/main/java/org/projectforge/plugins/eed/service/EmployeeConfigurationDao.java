package org.projectforge.plugins.eed.service;

import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.plugins.eed.ExtendEmployeeDataPluginUserRightId;
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
    userRightId = ExtendEmployeeDataPluginUserRightId.PLUGIN_EXTENDEMPLOYEEDATA;
  }

  @Override
  public EmployeeConfigurationDO newInstance()
  {
    return new EmployeeConfigurationDO();
  }
}
