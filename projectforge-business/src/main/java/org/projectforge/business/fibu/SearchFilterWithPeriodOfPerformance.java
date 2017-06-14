package org.projectforge.business.fibu;

import java.util.Date;

public interface SearchFilterWithPeriodOfPerformance
{
  Date getPeriodOfPerformanceStartDate();

  void setPeriodOfPerformanceStartDate(final Date periodOfPerformanceStartDate);

  Date getPeriodOfPerformanceEndDate();

  void setPeriodOfPerformanceEndDate(final Date periodOfPerformanceEndDate);
}
