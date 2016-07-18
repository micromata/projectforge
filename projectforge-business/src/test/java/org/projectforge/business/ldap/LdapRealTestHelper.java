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

package org.projectforge.business.ldap;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.test.JUnitLDAPTestWrapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test helper class for do some tests with a real LDAP test system. The LDAP system settings have to be set in
 * $USER.HOME/rojectForge/testldapConfig.xml:
 * 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8" ?&gt;
 * &lt;ldapConfig&gt;
 *     &lt;server&gt;ldaps://192.168.76.177&lt;/server&gt;
 *     &lt;port&gt;636&lt;/port&gt;
 *     &lt;userBase&gt;ou=pf-test-users&lt;/userBase&gt;
 *     &lt;baseDN&gt;dc=projectforge,dc=org&lt;/baseDN&gt;
 *     &lt;authentication&gt;simple&lt;/authentication&gt;
 *     &lt;managerUser&gt;cn=testuser&lt;/managerUser&gt;
 *     &lt;managerPassword&gt;secret&lt;/managerPassword&gt;
 *     &lt;sslCertificateFile&gt;/Users/kai/ProjectForge/testldap.cert&lt;/sslCertificateFile&gt;
 * &lt;/ldapConfig&gt;
 * </pre>
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class LdapRealTestHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapRealTestHelper.class);

  private static final String CONFIG_FILE = System.getProperty("user.home") + "/ProjectForge/testldapConfig.xml";

  @Autowired
  LdapServiceImpl ldapService;

  LdapConfig ldapConfig;

  LdapConnector ldapConnector;

  LdapUserDao ldapUserDao;

  LdapPersonDao ldapPersonDao;

  LdapGroupDao ldapGroupDao;

  LdapOrganizationalUnitDao ldapOrganizationalUnitDao;

  private String userPath, groupPath;

  public LdapRealTestHelper setup()
  {
    if (ldapConfig == null) {
      LdapServiceImpl ldapServiceImpl = new LdapServiceImpl();
      ldapServiceImpl.authentication = "simple";
      ldapServiceImpl.baseDN = "dc=example, dc=org";
      ldapServiceImpl.groupBase = "ou=groups";
      ldapServiceImpl.managerPassword = "secret";
      ldapServiceImpl.managerUser = "uid=admin,ou=system";
      ldapServiceImpl.port = 1024;
      ldapServiceImpl.server = "localhost";
      ldapServiceImpl.userBase = "ou=users";
      ldapServiceImpl.init();
      ldapService = ldapServiceImpl;
      ldapConfig = ldapService.getLdapConfig();
      ldapConnector.ldapService = ldapService;
    }
    if (isAvailable() == true) {
      if (ldapOrganizationalUnitDao == null) {
        ldapPersonDao = new LdapPersonDao();
        ldapPersonDao.setLdapConnector(ldapConnector);
        ldapUserDao = new LdapUserDao();
        ldapUserDao.setLdapConnector(ldapConnector);
        ldapUserDao.setLdapPersonDao(ldapPersonDao);
        ldapOrganizationalUnitDao = new LdapOrganizationalUnitDao();
        ldapOrganizationalUnitDao.setLdapConnector(ldapConnector);
        ldapGroupDao = new LdapGroupDao();
        ldapGroupDao.setLdapConnector(ldapConnector);

        //ldapOrganizationalUnitDao.createIfNotExist(getUserPath(), "Test area for tests of ProjectForge.");
        //ldapOrganizationalUnitDao.createIfNotExist(LdapUserDao.DEACTIVATED_SUB_CONTEXT, "for deactivated users.",
         //   getUserPath());
        //ldapOrganizationalUnitDao.createIfNotExist(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT, "for restricted users.",
         //   getUserPath());
        //ldapOrganizationalUnitDao.createIfNotExist(getGroupPath(), "Test area for tests of ProjectForge.");
      }
    }
    return this;
  }

  public void tearDown()
  {
    if (isAvailable() == true) {
      ldapOrganizationalUnitDao.deleteIfExists(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getUserPath());
      ldapOrganizationalUnitDao.deleteIfExists(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT, getUserPath());
      ldapOrganizationalUnitDao.deleteIfExists(getUserPath());
      ldapOrganizationalUnitDao.deleteIfExists(getGroupPath());
    }
  }

  String getUserPath()
  {
    if (userPath == null) {
      userPath = LdapUtils.getOrganizationalUnit(ldapConfig.getUserBase());
    }
    return userPath;
  }

  String getGroupPath()
  {
    if (groupPath == null) {
      groupPath = LdapUtils.getOrganizationalUnit(ldapConfig.getGroupBase());
    }
    return groupPath;
  }

  LdapRealTestHelper()
  {
    ldapConfig = readConfig();
    ldapConnector = new LdapConnector(ldapConfig);
  }

  /**
   * @return true if the LDAP test system is available for tests, otherwise false.
   */
  boolean isAvailable()
  {
    return ldapConfig != null;
  }

  private LdapConfig readConfig()
  {
    if ( JUnitLDAPTestWrapper.ldapServerWrap != null) {
      LdapConfig ldapConfig = new LdapConfig();
      ldapConfig.setPort(JUnitLDAPTestWrapper.ldapServerWrap.getPort());
      ldapConfig.setServer(JUnitLDAPTestWrapper.ldapServerWrap.getTransports()[0].getAddress());
      ldapConfig.setManagerUser("uid=admin,ou=system");
      ldapConfig.setManagerPassword("");
      ldapConfig.setUserBase("ou=users");
      ldapConfig.setBaseDN("dc=example,dc=org");
      ldapConfig.setGroupBase("ou=groups");
      return ldapConfig;
    } else {
      final File configFile = new File(CONFIG_FILE);
      if (configFile.canRead() == false) {
        return null;
      }
      log.info("Reading LDAP configuration file for test cases: " + configFile.getPath());
      final XmlObjectReader reader = new XmlObjectReader();
      final AliasMap aliasMap = new AliasMap();
      aliasMap.put(LdapConfig.class, "ldapConfig");
      reader.setAliasMap(aliasMap);
      AccountingConfig.registerXmlObjects(reader, aliasMap);
      String xml = null;
      try {
        xml = FileUtils.readFileToString(configFile, "UTF-8");
      } catch (final IOException ex) {
        log.error(ex.getMessage(), ex);
        throw new IllegalArgumentException("Cannot read config file '" + CONFIG_FILE + "' properly : " + ex.getMessage(),
                ex);
      }
      if (xml == null) {
        throw new IllegalArgumentException("Cannot read from config file: '" + CONFIG_FILE + "'.");
      }
      try {
        final LdapConfig cfg = (LdapConfig) reader.read(xml);
        final String warnings = reader.getWarnings();
        if (StringUtils.isNotBlank(warnings) == true) {
          log.error(warnings);
        }
        return cfg;
      } catch (final Throwable ex) {
        throw new IllegalArgumentException("Cannot read config file '" + CONFIG_FILE + "' properly : " + ex.getMessage(),
                ex);
      }
    }
  }

}
