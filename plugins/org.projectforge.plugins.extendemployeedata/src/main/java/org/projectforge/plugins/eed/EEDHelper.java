package org.projectforge.plugins.eed;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

@Service
public class EEDHelper
{
  @Autowired
  private TimeableService<Integer, EmployeeTimedDO> timeableService;

  @Autowired
  private EmployeeDao employeeDao;

  public List<Integer> getDropDownYears()
  {
    // do not cache the years because this is a long lasting service and the years could change in the meantime
    final List<Integer> years = timeableService.getAvailableStartTimeYears(employeeDao.internalLoadAll());
    final Integer actualYear = new GregorianCalendar().get(Calendar.YEAR);
    if (years.contains(actualYear) == false) {
      years.add(actualYear);
    }
    if (years.contains(actualYear + 1) == false) {
      years.add(actualYear + 1);
    }
    return years
        .stream()
        .sorted()
        .collect(Collectors.toList());
  }

}
