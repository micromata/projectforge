package org.projectforge.business.vacation.job;

import java.util.Collection;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.service.VacationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UpdateVacationFromLastYearJob
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(UpdateVacationFromLastYearJob.class);

  @Autowired
  private VacationService vacationService;

  @Autowired
  private EmployeeService employeeService;

  @Scheduled(cron = "0 0 23 31 12 *")
  public void updateNewVacationDaysFromLastYear()
  {
    log.info("Update vacation days from last year");
    Collection<EmployeeDO> activeEmployees = employeeService.findAllActive(false);
    activeEmployees.forEach(emp -> vacationService.updateUsedNewVacationDaysFromLastYear(emp));
    log.info("Update vacation days from last year DONE");
  }

}
