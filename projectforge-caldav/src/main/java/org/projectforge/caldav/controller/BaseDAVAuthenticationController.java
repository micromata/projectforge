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

package org.projectforge.caldav.controller;

import io.milton.annotations.Authenticate;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.caldav.model.User;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by blumenstein on 21.11.16.
 */
public class BaseDAVAuthenticationController {
    private boolean autowired = false;

    protected void ensureAutowire() {
        if (!autowired) {
            // Late initialization is required. ApplicationContext isn't available in constructor.
            ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
            autowired = true;
        }
    }

    /**
     * Function as Java code needed, because Milton fails while checking return type of this method (java.lang.Boolean required).
     * @param user
     * @param requestedPassword
     * @return
     */
    @Authenticate
    public Boolean authenticate(final User user, final String requestedPassword) {
        log.info("Trying to authenticate user '" + user.getUsername()  + "'.");
        final PFUserDO contextUser = ThreadLocalUserContext.getUser();
        if (contextUser == null) {
          return false;
        }
        return StringUtils.equals(contextUser.getUsername(), user.getUsername());
    }

    private static Logger log = LoggerFactory.getLogger(BaseDAVController.class);
}
