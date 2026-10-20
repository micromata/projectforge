/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.framework.configuration;

import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.orga.ContractType;
import org.projectforge.framework.calendar.ConfigureHoliday;
import org.projectforge.framework.xmlstream.AliasMap;
import org.projectforge.framework.xmlstream.XmlObject;
import org.projectforge.framework.xmlstream.XmlObjectReader;

import java.util.List;

/**
 * The content of the former config.xml in the base dir, read only for taking it over into the configuration
 * parameters and properties (see ConfigXmlMigration). Fields not used anymore (database and logging directory,
 * portlet mode) are declared only for reading old files without warnings.
 */
@XmlObject(alias = "config")
public class LegacyConfigXml {
    public static final String CONFIG_XML_FILE = "config.xml";

    private String jiraBrowseBaseUrl;

    private List<ConfigureJiraServer> jiraServers;

    private List<ConfigureHoliday> holidays;

    private String databaseDirectory;

    private String loggingDirectory;

    private String jcrDirectory;

    private String workingDirectory;

    private String backupDirectory;

    private String tempDirectory;

    private List<ContractType> contractTypes;

    private boolean portletMode;

    private AccountingConfig accountingConfig;

    /**
     * @param xml The content of config.xml.
     * @return The read configuration.
     */
    public static LegacyConfigXml read(final String xml) {
        final XmlObjectReader reader = new XmlObjectReader();
        final AliasMap aliasMap = new AliasMap();
        reader.setAliasMap(aliasMap);
        reader.initialize(LegacyConfigXml.class);
        reader.initialize(ConfigureJiraServer.class);
        reader.initialize(ConfigureHoliday.class);
        reader.initialize(ContractType.class);
        AccountingConfig.registerXmlObjects(reader, aliasMap);
        // Fix mis-spelled new year's eve (it's not Mr. Stallone's day ;-)
        return (LegacyConfigXml) reader.read(xml.replace("SYLVESTER", "NEW_YEARS_EVE")); // Used before 2022/03/26
    }

    public String getJiraBrowseBaseUrl() {
        return jiraBrowseBaseUrl;
    }

    public List<ConfigureJiraServer> getJiraServers() {
        return jiraServers;
    }

    public List<ConfigureHoliday> getHolidays() {
        return holidays;
    }

    public String getJcrDirectory() {
        return jcrDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public List<ContractType> getContractTypes() {
        return contractTypes;
    }

    public AccountingConfig getAccountingConfig() {
        return accountingConfig;
    }
}
