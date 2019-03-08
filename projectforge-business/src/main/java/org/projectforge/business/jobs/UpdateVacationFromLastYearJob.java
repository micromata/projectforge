package org.projectforge.business.jobs;

import java.util.Calendar;
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
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UpdateVacationFromLastYearJob.class);

  @Autowired
  private VacationService vacationService;

  @Autowired
  private EmployeeService employeeService;
  
  @Scheduled(cron = "${projectforge.cron.updateVacationLastYear:0 0 20 31 12 *}")
  public void updateNewVacationDaysFromLastYear()
  {
    log.info("Update vacation days from last year job started.");
    Calendar now = Calendar.getInstance();
    Collection<EmployeeDO> activeEmployees = employeeService.findAllActive(false);
    activeEmployees.forEach(emp -> {
      try {
        vacationService.updateVacationDaysFromLastYearForNewYear(emp, now.get(Calendar.YEAR));
      } catch (Exception e) {
        log.error("Exception while updating vacation from last year for employee: " + emp.getUser().getFullname(), e);
      }
    });
    log.info("Update vacation days from last year job finished.");
  }

}
