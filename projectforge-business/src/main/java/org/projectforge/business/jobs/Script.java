package org.projectforge.business.jobs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.continuousdb.DatabaseResultRow;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.time.DateHelper;

public class Script
{
  public void updateNewVacationDaysFromLastYear()
  {
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UpdateVacationFromLastYearJob.class);

    DatabaseService databaseService = ApplicationContextProvider.getApplicationContext().getBean(DatabaseService.class);
    EmployeeService employeeService = ApplicationContextProvider.getApplicationContext().getBean(EmployeeService.class);
    EmployeeDao employeeDao = ApplicationContextProvider.getApplicationContext().getBean(EmployeeDao.class);
    VacationDao vacationDao = ApplicationContextProvider.getApplicationContext().getBean(VacationDao.class);
    VacationService vacationService = ApplicationContextProvider.getApplicationContext().getBean(VacationService.class);
    List<Integer> editedIds = Arrays
        .asList(8231204, 8231666, 9171119, 8232429, 8231274, 8231981, 8232261, 8231218, 8231267, 8231456, 8008913, 8231687, 8232506, 8232023, 8231246, 8232072,
            8231232, 8231729, 8231568, 8232044, 8231225, 8540081, 8232016, 9380988, 8231491, 8231645, 9665858, 10090494, 8232030, 8231211, 9949453, 11082913,
            8231638, 8231617, 10202225, 8232170, 8232275, 8231260, 10770948, 10081008, 8232310, 8747790, 8231540, 8231253, 8232478, 8231365, 8231589, 8231379,
            8231358, 8231351);

    log.info("Correct vacation days from last year job started.");

    Collection<EmployeeDO> activeEmployees = employeeService.findAllActive(false);
    for (EmployeeDO employee : activeEmployees) {
      if (editedIds.contains(employee.getId())) {
        continue;
      }
      List<DatabaseResultRow> resultList = databaseService.query(
          "select hist.createdat, attr.propertyname, attr.value from t_pf_history hist join t_pf_history_attr attr on attr.master_fk = hist.pk where hist.entity_id = "
              + employee.getId() + " and propertyname like 'attrs.previousyearleaveused%' order by hist.createdat desc");
      BigDecimal valueToAdd = BigDecimal.ZERO;
      for (DatabaseResultRow row : resultList) {
        boolean setValue = false;
        String propertynameValue = (String) row.getEntry(1).getValue();
        String value = (String) row.getEntry(2).getValue();
        if ("attrs.previousyearleaveused:ov".equals(propertynameValue) && StringUtils.isNotBlank(value) && setValue == false) {
          valueToAdd = new BigDecimal(value);
          setValue = true;
        }
      }

      BigDecimal availableVacationdaysFromActualYear = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class);
      if (availableVacationdaysFromActualYear == null) {
        availableVacationdaysFromActualYear = BigDecimal.ZERO;
      }
      availableVacationdaysFromActualYear.add(valueToAdd);
      employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), availableVacationdaysFromActualYear);

      // find approved vacations in new year
      Calendar from = Calendar.getInstance();
      from.setTimeZone(DateHelper.UTC);
      from.set(2018, Calendar.JANUARY, 1, 0, 0, 0);
      Date to = vacationService.getEndDateVacationFromLastYear().getTime();
      List<VacationDO> vacationNewYear = vacationDao.getVacationForPeriod(employee, from.getTime(), to, false);

      BigDecimal usedInNewYear = BigDecimal.ZERO;

      for (VacationDO vacation : vacationNewYear) {
        if (vacation.getStatus() != VacationStatus.APPROVED) {
          continue;
        }

        // compute used days until EndDateVacationFromLastYear
        BigDecimal days = vacationService
            .getVacationDays(vacation.getStartDate(), vacation.getEndDate().after(to) ? to : vacation.getEndDate(), vacation.getHalfDay());
        if (days == null) {
          days = BigDecimal.ZERO;
        }
        usedInNewYear = usedInNewYear.add(days);
      }

      // compute used days
      final BigDecimal usedDays = availableVacationdaysFromActualYear.compareTo(usedInNewYear) < 1 ? availableVacationdaysFromActualYear : usedInNewYear;

      employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), usedDays);
      employeeDao.internalUpdate(employee);
    }

    log.info("Correct vacation days from last year job finished.");
  }

}
