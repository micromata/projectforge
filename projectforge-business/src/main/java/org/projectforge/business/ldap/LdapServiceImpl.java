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

package org.projectforge.business.ldap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LdapServiceImpl implements LdapService
{

  @Value("${projectforge.ldap.server}")
  String server;

  @Value("${projectforge.ldap.baseDN}")
  String baseDN;

  @Value("${projectforge.ldap.managerUser}")
  String managerUser;

  @Value("${projectforge.ldap.managerPassword}")
  String managerPassword;

  @Value("${projectforge.ldap.port}")
  Integer port;

  @Value("${projectforge.ldap.sslCertificateFile}")
  String sslCertificateFile;

  @Value("${projectforge.ldap.groupBase}")
  String groupBase;

  @Value("${projectforge.ldap.userBase}")
  String userBase;

  @Value("${projectforge.ldap.authentication}")
  String authentication;

  @Value("${projectforge.ldap.posixAccountsDefaultGidNumber}")
  Integer posixAccountsDefaultGidNumber;

  @Value("${projectforge.ldap.sambaAccountsSIDPrefix}")
  String sambaAccountsSIDPrefix;

  @Value("${projectforge.ldap.sambaAccountsPrimaryGroupSID}")
  Integer sambaAccountsPrimaryGroupSID;

  private LdapConfig ldapConfig;

  @PostConstruct
  public void init()
  {
    this.ldapConfig = new LdapConfig();
    this.ldapConfig.setServer(server);
    this.ldapConfig.setBaseDN(baseDN);
    this.ldapConfig.setManagerUser(managerUser);
    this.ldapConfig.setManagerPassword(managerPassword);
    this.ldapConfig.setPort(port);
    this.ldapConfig.setSslCertificateFile(sslCertificateFile);
    this.ldapConfig.setGroupBase(groupBase);
    this.ldapConfig.setUserBase(userBase);
    this.ldapConfig.setAuthentication(authentication);
    LdapPosixAccountsConfig posixAccountsConfig = new LdapPosixAccountsConfig();
    if (posixAccountsDefaultGidNumber != null) {
      posixAccountsConfig.setDefaultGidNumber(posixAccountsDefaultGidNumber);
    }
    this.ldapConfig.setPosixAccountsConfig(posixAccountsConfig);
    LdapSambaAccountsConfig sambaAccountsConfig = new LdapSambaAccountsConfig();
    sambaAccountsConfig.setSambaSIDPrefix(sambaAccountsSIDPrefix);
    if (sambaAccountsPrimaryGroupSID != null) {
      sambaAccountsConfig.setDefaultSambaPrimaryGroupSID(sambaAccountsPrimaryGroupSID);
    }
    this.ldapConfig.setSambaAccountsConfig(sambaAccountsConfig);
  }

  /**
   * @return the ldapConfig
   */
  @Override
  public LdapConfig getLdapConfig()
  {
    return ldapConfig;
  }

  /**
   * For test use only
   * 
   * @param ldapConfig the ldapConfig to set
   * @return this for chaining.
   */
  public void setLdapConfig(final LdapConfig ldapConfig)
  {
    this.ldapConfig = ldapConfig;
  }

}
