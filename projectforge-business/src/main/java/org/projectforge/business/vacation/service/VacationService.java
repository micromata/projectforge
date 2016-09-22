package org.projectforge.business.vacation.service;

import java.util.Date;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.IPersistenceService;

/**
 * Access to vacation.
 * 
 * @author Florian Blumenstein
 *
 */
public interface VacationService extends IPersistenceService<VacationDO>, IDao<VacationDO>
{

  /**
   * Getting all vacations for given employee and a time period.
   * 
   * @param employee
   * @param startDate
   * @param endDate
   * @return List of vacations
   */
  List<VacationDO> getVacationForDate(EmployeeDO employee, Date startDate, Date endDate);

}
