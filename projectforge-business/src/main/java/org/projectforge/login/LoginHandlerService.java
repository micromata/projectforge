package org.projectforge.login;

import org.projectforge.business.ldap.LdapMasterLoginHandler;
import org.projectforge.business.ldap.LdapSlaveLoginHandler;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginDefaultHandler;
import org.projectforge.business.login.LoginHandler;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserGroupCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class LoginHandlerService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginHandlerService.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${projectforge.login.handlerClass}")
  private String loginHandlerClass;

  private LoginHandler loginHandler;

  @PostConstruct
  public void init() {
    switch (getLoginHandlerClass()) {
      case "LdapMasterLoginHandler":
        this.loginHandler = applicationContext.getBean(LdapMasterLoginHandler.class);
        break;
      case "LdapSlaveLoginHandler":
        this.loginHandler = applicationContext.getBean(LdapSlaveLoginHandler.class);
        break;
      default:
        this.loginHandler = applicationContext.getBean(LoginDefaultHandler.class);
    }
    Login.getInstance().setLoginHandler(loginHandler);
    loginHandler.initialize();
  }

  public TenantRegistry getTenantRegistry() {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  public UserGroupCache getUserGroupCache() {
    return getTenantRegistry().getUserGroupCache();
  }

  /**
   * If given then this login handler will be used instead of {@link LoginDefaultHandler}. For ldap please use e. g.
   * org.projectforge.ldap.LdapLoginHandler.
   *
   * @return the loginHandlerClass or "" if not given
   */
  private String getLoginHandlerClass() {
    if (loginHandlerClass != null) {
      return loginHandlerClass;
    }
    return "";
  }

  public LoginHandler getLoginHandler() {
    return loginHandler;
  }
}
