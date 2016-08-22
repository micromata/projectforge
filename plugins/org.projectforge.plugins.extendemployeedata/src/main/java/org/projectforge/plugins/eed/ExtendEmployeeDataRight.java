package org.projectforge.plugins.eed;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class ExtendEmployeeDataRight extends UserRightAccessCheck<EmployeeConfigurationDO>
{
  public ExtendEmployeeDataRight(AccessChecker accessChecker)
  {
    super(accessChecker, ExtendEmployeeDataPluginUserRightId.PLUGIN_EXTENDEMPLOYEEDATA, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  /**
   * @return true if the owner is equals to the logged-in user, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final EmployeeConfigurationDO obj, final EmployeeConfigurationDO oldObj,
      final OperationType operationType)
  {
    return true;
    /*
    final EmployeeConfigurationDO timesheet = oldObj != null ? oldObj : obj;
    if (timesheet == null) {
      return true; // General insert and select access given by default.
    }*/
  }
}
