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

package org.projectforge.business.meb;

import org.projectforge.business.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Can be called nightly or every 10 minutes for getting all or only recent MEB mails from the configured mail server
 * for checking any missing MEB message. If no MEB mail account is configured, the job does nothing.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Service
public class MebJobExecutor
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MebJobExecutor.class);

  @Autowired
  private MebMailClient mebMailClient;

  @Autowired
  private ConfigurationService configurationService;

  /**
   * This algorithm avoids multiple entries.
   * 
   * @param importAllMails If false then only recent e-mails will be imported, otherwise all e-mails will be checked for
   *          import.
   */
  public void execute(final boolean importAllMails)
  {
    if (mebMailClient == null) {
      log.error("Job not configured, aborting.");
    }
    if (configurationService.isMebMailAccountConfigured() == false) {
      return;
    }
    synchronized (mebMailClient) {
      log.info("MEB job started in '" + (importAllMails == true ? "read-all" : "read-recent") + "' mode.");
      int counter;
      if (importAllMails == true) {
        counter = mebMailClient.getNewMessages(false, false);
      } else {
        counter = mebMailClient.getNewMessages(true, true);
      }
      log.info("MEB job finished successfully, " + counter + " new messages imported.");
    }
  }

}
