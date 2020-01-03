/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.text.SimpleDateFormat;

public class UpdateVacationFromLastYearJobCronCheckTest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(UpdateVacationFromLastYearJobCronCheckTest.class);

  @Test
  @Disabled
  public void testCronExpression()
  {
    CronSequenceGenerator cron1 = new CronSequenceGenerator("0 0 23 31 12 *");
    PFDateTime dt = PFDateTime.now().plusDays(2);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

    log.error("current date " + sdf.format(dt.getUtilDate()));

    log.error("Next cron trigger date cron1 " + cron1.next(dt.getUtilDate()));
  }
}
