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
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.ConfigurationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Should be initialized on start-up and will be called every time if config.xml is reread. This class is needed for
 * initialization of the spring beans with properties configured in config.xml.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class LdapConnector implements ConfigurationListener
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapConnector.class);

  @Autowired
  LdapService ldapService;

  private LdapConfig ldapConfig;

  private boolean initialized;

  /**
   * Don't call this constructor unless you really know what you're doing. This LdapHelper is a singleton and is
   * available via IOC.
   */
  public LdapConnector()
  {
  }

  private void init()
  {
    if (initialized == true) {
      return;
    }
    synchronized (this) {
      if (initialized == true) {
        return;
      }
      ConfigXml.getInstance().register(this);
      afterRead();
      initialized = true;
    }
  }

  private Hashtable<String, String> createEnv(final String user, final String password)
  {
    // Set up the environment for creating the initial context
    final Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, ldapConfig.getCompleteServerUrl());
    final String authentication = ldapConfig.getAuthentication();
    if (StringUtils.isNotBlank(authentication) == true) {
      env.put(Context.SECURITY_AUTHENTICATION, ldapConfig.getAuthentication());
      if ("none".equals(authentication) == false && user != null && password != null) {
        env.put(Context.SECURITY_PRINCIPAL, user);
        env.put(Context.SECURITY_CREDENTIALS, password);
      }
    }
    if (ldapConfig != null && StringUtils.isNotBlank(ldapConfig.getSslCertificateFile()) == true) {
      env.put("java.naming.ldap.factory.socket", "org.projectforge.business.ldap.MySSLSocketFactory");
    }
    log.info("Trying to connect the LDAP server: url=["
        + ldapConfig.getCompleteServerUrl()
        + "], authentication=["
        + ldapConfig.getAuthentication()
        + "], principal=["
        + user
        + "]");
    return env;
  }

  public String getBase()
  {
    init();
    return ldapConfig.getBaseDN();
  }

  public LdapContext createContext()
  {
    init();
    final Hashtable<String, String> env;
    final String authentication = ldapConfig.getAuthentication();
    if ("none".equals(authentication) == false) {
      env = createEnv(ldapConfig.getManagerUser(), ldapConfig.getManagerPassword());
    } else {
      env = createEnv(null, null);
    }
    try {
      final LdapContext ctx = new InitialLdapContext(env, null);
      return ctx;
    } catch (final NamingException ex) {
      log.error("While trying to connect LDAP initally: " + ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  public LdapContext createContext(final String username, final String password) throws NamingException
  {
    init();
    final Hashtable<String, String> env = createEnv(username, password);
    final LdapContext ctx = new InitialLdapContext(env, null);
    return ctx;
  }

  /**
   * Used by test class.
   * 
   * @param ldapConfig
   */
  LdapConnector(final LdapConfig ldapConfig)
  {
    this.ldapConfig = ldapConfig;
    if (this.ldapConfig != null && StringUtils.isNotBlank(this.ldapConfig.getSslCertificateFile()) == true) {
      // Try to load SSL certificate.
      MyTrustManager.getInstance().addCertificate("ldap", new File(this.ldapConfig.getSslCertificateFile()));
    }
    initialized = true;
  }

  /**
   * @see org.projectforge.framework.configuration.ConfigurationListener#afterRead()
   */
  @Override
  public void afterRead()
  {
    this.ldapConfig = ldapService.getLdapConfig();
    if (this.ldapConfig != null && StringUtils.isNotBlank(this.ldapConfig.getSslCertificateFile()) == true) {
      // Try to load SSL certificate.
      MyTrustManager.getInstance().addCertificate("ldap", new File(this.ldapConfig.getSslCertificateFile()));
    }
  }

  /**
   * @return the ldapConfig
   */
  LdapConfig getLdapConfig()
  {
    return ldapConfig;
  }
}
