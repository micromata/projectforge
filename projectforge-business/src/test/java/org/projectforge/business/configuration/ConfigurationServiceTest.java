/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.configuration;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationServiceTest extends AbstractTestBase {
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ConfigurationDao configurationDao;

    @Override
    protected void afterAll() {
        recreateDataBase();
    }

    @Test
    public void testGetMinPasswordLength() {
        final long defaultMinPwLen = ConfigurationParam.MIN_PASSWORD_LENGTH.getDefaultLongValue();

        // default
        assertEquals(defaultMinPwLen, configurationService.getMinPasswordLength());

        final ConfigurationDO minPwLenEntry = configurationDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH);
        minPwLenEntry.setLongValue(16L);
        configurationDao.update(minPwLenEntry, false);
        assertEquals(16, configurationService.getMinPasswordLength());

        // null -> use default
        minPwLenEntry.setLongValue(null);
        configurationDao.update(minPwLenEntry, false);
        assertEquals(defaultMinPwLen, configurationService.getMinPasswordLength());
    }

    /**
     * Test flag password change verification on newly entered password, that passwords have to change.
     */
    @Test
    public void testGetFlagPasswordChange() {
        final boolean defaultFlagPwChange = ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE.getDefaultBooleanValue();

        // default
        assertEquals(defaultFlagPwChange, configurationService.getFlagCheckPasswordChange());

        final ConfigurationDO flagPwChange = configurationDao.getEntry(ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE);
        flagPwChange.setBooleanValue(false);
        configurationDao.update(flagPwChange, false);
        assertEquals(false, configurationService.getFlagCheckPasswordChange());
    }
}
