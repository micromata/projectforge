package org.projectforge.plugins.ffp;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;

public class FinancialFairPlayDebtRight extends UserRightAccessCheck<FFPDebtDO>
{
  public FinancialFairPlayDebtRight(AccessChecker accessChecker)
  {
    super(accessChecker, FinancialFairPlayPluginUserRightId.PLUGIN_FINANCIALFAIRPLAY, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  /**
   * @return true if the owner is equals to the logged-in user, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final FFPDebtDO obj, final FFPDebtDO oldObj,
      final OperationType operationType)
  {
    return true;
  }
}
