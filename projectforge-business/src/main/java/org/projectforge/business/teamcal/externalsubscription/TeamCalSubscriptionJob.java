/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.teamcal.externalsubscription;

import org.projectforge.business.jobs.AbstractCronJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
@Component
public class TeamCalSubscriptionJob extends AbstractCronJob
{

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalSubscriptionJob.class);

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  public void execute(final JobExecutionContext context) throws JobExecutionException
  {
    teamEventExternalSubscriptionCache.updateCache();
  }

  @Override
  protected void wire(final JobExecutionContext context)
  {
    // nothing to do here
  }
}
