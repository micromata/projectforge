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

package org.projectforge.business.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class AbstractCronJob implements Job
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractCronJob.class);

  protected abstract void wire(final JobExecutionContext context);

  protected Object wire(final JobExecutionContext context, final String key)
  {
    final Object result = context.getMergedJobDataMap().get(key);
    if (result == null) {
      log.fatal("Mis-configuration of scheduler in applicationContext-web.xml: '" + key + "' is not availabe.");
    }
    return result;
  }
}
