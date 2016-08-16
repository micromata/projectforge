package org.projectforge.plugins.eed;

import org.projectforge.framework.persistence.api.BaseDao;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class EmployeeGeneralValueDao extends BaseDao<EmployeeGeneralValueDO>
{

  protected EmployeeGeneralValueDao()
  {
    super(EmployeeGeneralValueDO.class);
    userRightId = ExtendEmployeeDataPluginUserRightId.PLUGIN_EXTENDEMPLOYEEDATA;
  }

  @Override public EmployeeGeneralValueDO newInstance()
  {
    return new EmployeeGeneralValueDO();
  }
}
