/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.jobs;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.jupiter.api.Disabled;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.junit.jupiter.api.Test;

public class UpdateVacationFromLastYearJobCronCheckTest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UpdateVacationFromLastYearJobCronCheckTest.class);

  @Test
  @Disabled
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
