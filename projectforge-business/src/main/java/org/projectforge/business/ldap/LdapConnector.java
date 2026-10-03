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

package org.projectforge.business.ldap;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.ConfigurationListener;
import org.projectforge.framework.integration.IntegrationConfig;
import org.projectforge.framework.integration.IntegrationErrors;
import org.projectforge.framework.integration.IntegrationTimeouts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.util.Hashtable;

/**
 * Should be initialized on start-up and will be called every time if config.xml is reread. This class is needed for
 * initialization of the spring beans with properties configured in config.xml.
 *
 * @author Kai Reinhard
 */
@Component
public class LdapConnector implements ConfigurationListener {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapConnector.class);

  @Autowired
  LdapService ldapService;

  private LdapConfig ldapConfig;

  private boolean initialized;

  /**
   * Don't call this constructor unless you really know what you're doing. This LdapHelper is a singleton and is
   * available via IOC.
   */
  public LdapConnector() {
  }

  private void init() {
    if (initialized) {
      return;
    }
    synchronized (this) {
      if (initialized) {
        return;
      }
      ConfigXml.getInstance().register(this);
      afterRead();
      initialized = true;
    }
  }

  private Hashtable<String, Object> createEnv(final String user, final char[] password) {
    // Set up the environment for creating the initial context
    final Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, ldapConfig.getCompleteServerUrl());
    final String authentication = ldapConfig.getAuthentication();
    if (StringUtils.isNotBlank(authentication)) {
      env.put(Context.SECURITY_AUTHENTICATION, ldapConfig.getAuthentication());
      if (!"none".equals(authentication)) {
        // Avoid null-value-attack (thanx to Sergej Michel, Micromata):
        final String userNotEmpty = StringUtils.isNotBlank(user) ? user : "<no-user>";
        final char[] passwordNotEmpty = password != null && password.length > 0 ? password : "<no-password>".toCharArray();
        env.put(Context.SECURITY_PRINCIPAL, userNotEmpty);
        env.put(Context.SECURITY_CREDENTIALS, passwordNotEmpty);
      }
    }
    if (ldapConfig != null && StringUtils.isNotBlank(ldapConfig.getSslCertificateFile())) {
      env.put("java.naming.ldap.factory.socket", "org.projectforge.business.ldap.MySSLSocketFactory");
    }
    // Without timeouts, an unreachable or hanging LDAP server blocks logins and syncs forever:
    final IntegrationTimeouts timeouts = IntegrationConfig.get(IntegrationConfig.LDAP);
    if (timeouts.getConnectTimeoutMs() > 0) {
      env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(timeouts.getConnectTimeoutMs()));
    }
    if (timeouts.getResponseTimeoutMs() > 0) {
      env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(timeouts.getResponseTimeoutMs()));
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

  public String getBase() {
    init();
    return ldapConfig.getBaseDN();
  }

  public LdapContext createContext() {
    init();
    final Hashtable<String, Object> env;
    final String authentication = ldapConfig.getAuthentication();
    if (!"none".equals(authentication)) {
      env = createEnv(ldapConfig.getManagerUser(), ldapConfig.getManagerPassword().toCharArray());
    } else {
      env = createEnv(null, null);
    }
    try {
      final LdapContext ctx = new InitialLdapContext(env, null);
      return ctx;
    } catch (final NamingException ex) {
      log.error("While trying to connect LDAP initially: " + ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }

  public LdapContext createContext(final String username, final char[] password) throws NamingException {
    init();
    final Hashtable<String, Object> env = createEnv(username, password);
    final LdapContext ctx = new InitialLdapContext(env, null);
    return ctx;
  }

  /**
   * Block executed with an open LDAP context, see {@link #withContext(String, char[], ContextCallback)}.
   */
  @FunctionalInterface
  public interface ContextCallback<T> {
    T apply(DirContext ctx) throws NamingException;
  }

  /**
   * Opens a context with the given credentials (bind), runs the block and closes the context in any case, so
   * no connection to the LDAP server is left open. The duration of the call is measured: slow calls and timeouts
   * are logged as warnings.
   *
   * @throws NamingException e.g. on invalid credentials, unreachable server or timeout.
   */
  public <T> T withContext(final String username, final char[] password, final ContextCallback<T> block)
      throws NamingException {
    final long start = System.currentTimeMillis();
    DirContext ctx = null;
    try {
      ctx = createContext(username, password);
      return block.apply(ctx);
    } catch (final NamingException ex) {
      if (IntegrationErrors.isTimeout(ex)) {
        log.warn("LDAP call of '" + username + "' timed out after " + (System.currentTimeMillis() - start) + "ms: "
            + ex.getMessage());
      }
      throw ex;
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (final NamingException ex) {
          log.warn("Error while closing LDAP context of '" + username + "': " + ex.getMessage());
        }
      }
      final long duration = System.currentTimeMillis() - start;
      final long responseTimeoutMs = IntegrationConfig.get(IntegrationConfig.LDAP).getResponseTimeoutMs();
      if (responseTimeoutMs > 0 && duration > responseTimeoutMs / 2) {
        log.warn("Slow LDAP call of '" + username + "': " + duration + "ms.");
      } else if (log.isDebugEnabled()) {
        log.debug("LDAP call of '" + username + "': " + duration + "ms.");
      }
    }
  }

  /**
   * Used by test class.
   *
   * @param ldapConfig
   */
  LdapConnector(final LdapConfig ldapConfig) {
    this.ldapConfig = ldapConfig;
    if (this.ldapConfig != null && StringUtils.isNotBlank(this.ldapConfig.getSslCertificateFile())) {
      // Try to load SSL certificate.
      MyTrustManager.getInstance().addCertificate("ldap", new File(this.ldapConfig.getSslCertificateFile()));
    }
    initialized = true;
  }

  /**
   * @see org.projectforge.framework.configuration.ConfigurationListener#afterRead()
   */
  @Override
  public void afterRead() {
    this.ldapConfig = ldapService.getLdapConfig();
    if (this.ldapConfig != null && StringUtils.isNotBlank(this.ldapConfig.getSslCertificateFile())) {
      // Try to load SSL certificate.
      MyTrustManager.getInstance().addCertificate("ldap", new File(this.ldapConfig.getSslCertificateFile()));
    }
  }

  /**
   * @return the ldapConfig
   */
  LdapConfig getLdapConfig() {
    return ldapConfig;
  }
}
