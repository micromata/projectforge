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
