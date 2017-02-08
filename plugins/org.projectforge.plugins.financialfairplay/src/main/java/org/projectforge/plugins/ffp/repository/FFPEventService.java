package org.projectforge.plugins.ffp.repository;

import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;

/**
 * Access to ffp event.
 *
 * @author Florian Blumenstein
 */
public interface FFPEventService extends IPersistenceService<FFPEventDO>, IDao<FFPEventDO>
{
  FFPEventDao getEventDao();

  FFPDebtDao getDebtDao();

  List<FFPDebtDO> calculateDebt(FFPEventDO event);

  List<FFPDebtDO> getDeptList(EmployeeDO currentEmployee);

  void createDept(FFPEventDO event);

  void updateDebtFrom(FFPDebtDO debt);

  void updateDebtTo(FFPDebtDO debt);

  Integer getOpenDebts(PFUserDO user);

  boolean debtExists(FFPEventDO event);
}
