package org.projectforge.business.jobs;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.springframework.scheduling.support.CronSequenceGenerator;
import org.testng.annotations.Test;

public class UpdateVacationFromLastYearJobCronCheckTest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UpdateVacationFromLastYearJobCronCheckTest.class);

  @Test
  public void testCronExpression()
  {
    CronSequenceGenerator cron1 = new CronSequenceGenerator("0 0 23 31 12 *");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, 2); // add two days to current date
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

    log.error("current date " + sdf.format(cal.getTime()));

    log.error("Next cron trigger date cron1 " + cron1.next(cal.getTime()));
  }
}
