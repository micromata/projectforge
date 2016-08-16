package org.projectforge.plugins.eed;

import org.apache.commons.lang.ObjectUtils;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class ExtendEmployeeDataRight extends UserRightAccessCheck<EmployeeGeneralValueDO>
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
  public boolean hasAccess(final PFUserDO user, final EmployeeGeneralValueDO obj, final EmployeeGeneralValueDO oldObj,
      final OperationType operationType)
  {
    return true;
    /*
    final EmployeeGeneralValueDO timesheet = oldObj != null ? oldObj : obj;
    if (timesheet == null) {
      return true; // General insert and select access given by default.
    }*/
  }
}
