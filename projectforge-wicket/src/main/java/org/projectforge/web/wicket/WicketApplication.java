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

package org.projectforge.web.wicket;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.ConverterLocator;
import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.mapper.StalePageException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.pageStore.IDataStore;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.resource.loader.BundleStringResourceLoader;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.time.Duration;
import org.projectforge.Const;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.ldap.LdapMasterLoginHandler;
import org.projectforge.business.ldap.LdapSlaveLoginHandler;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginDefaultHandler;
import org.projectforge.business.login.LoginHandler;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.multitenancy.TenantsCache;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.web.LoginService;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.session.MySession;
import org.projectforge.web.teamcal.integration.TeamCalCalendarPage;
import org.projectforge.web.wicket.converter.MyDateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import de.micromata.less.LessWicketApplicationInstantiator;
import de.micromata.wicket.request.mapper.PageParameterAwareMountedMapper;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start
 * class.
 *
 * @see org.projectforge.start.AbstractStartHelper.demo.Start#main(String[])
 */
@Controller
public class WicketApplication extends WebApplication implements WicketApplicationInterface/* , SmartLifecycle */
{
  public static final String RESOURCE_BUNDLE_NAME = "I18nResources";

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WicketApplication.class);

  private static final Map<Class<? extends Page>, String> mountedPages = new HashMap<>();

  public static Class<? extends WebPage> DEFAULT_PAGE = CalendarPage.class;

  private static Boolean stripWicketTags;

  private static String alertMessage;

  private static Boolean developmentMode;

  private static Boolean testsystemMode;

  private static String testsystemColor;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private DatabaseUpdateService databaseUpdater;

  @Autowired
  private PluginAdminService pluginAdminService;

  @Autowired
  private LoginService loginService;

  @Autowired
  private ConfigurationService configurationService;

  private LoginHandler loginHandler;

  @Value("${projectforge.base.dir}")
  private String baseDir;

  private ProjectForgeApp projectForgeApp;

  /**
   * Constructor
   */
  public WicketApplication()
  {
    super();
  }

  public static Boolean getTestsystemMode()
  {
    return testsystemMode;
  }

  @Value("${projectforge.testsystemMode}")
  public void setTestsystemMode(Boolean testsystemMode)
  {
    WicketApplication.testsystemMode = testsystemMode;
  }

  public static String getTestsystemColor()
  {
    return testsystemColor;
  }

  @Value("${projectforge.testsystemColor}")
  public void setTestsystemColor(String testsystemColor)
  {
    WicketApplication.testsystemColor = testsystemColor;
  }

  /**
   * At application start the flag developmentMode is perhaps not already set. If possible please use
   * {@link #isDevelopmentSystem()} instead.<br/>
   * Please use {@link WebConfiguration#isDevelopmentMode()}.
   */
  public static Boolean internalIsDevelopmentMode()
  {
    return developmentMode;
  }

  /**
   * This method should only be called in test cases!
   *
   * @param upAndRunning the upAndRunning to set
   */
  public static void internalSetUpAndRunning(final boolean upAndRunning)
  {
    ProjectForgeApp.getInstance().internalSetUpAndRunning(upAndRunning);
  }

  /**
   * Please don't use this method, use {@link WicketUtils#getDefaultPage()} instead.
   *
   * @return
   */
  public static Class<? extends WebPage> internalGetDefaultPage()
  {
    return DEFAULT_PAGE;
  }

  /**
   * Use this method only if you want to change the default page (if no other is defined in config.xml).
   *
   * @param defaultPage
   */
  public static void setDefaultPage(final Class<? extends WebPage> defaultPage)
  {
    DEFAULT_PAGE = defaultPage;
  }

  public static String getBookmarkableMountPath(final Class<? extends Page> pageClass)
  {
    return mountedPages.get(pageClass);
  }

  /**
   * Returns the alert message, if exists. The alert message will be displayed on every screen (red on top) and is
   * edit-able via Administration -> System.
   */
  public static String getAlertMessage()
  {
    if (UserFilter.isUpdateRequiredFirst() == true) {
      return "Maintenance mode: Please restart ProjectForge after finishing."
          + (alertMessage != null ? " " + alertMessage : "");
    } else {
      return alertMessage;
    }
  }

  /**
   * @param alertMessage
   * @see #getAlertMessage()
   */
  public static void setAlertMessage(final String alertMessage)
  {
    WicketApplication.alertMessage = alertMessage;
  }

  public static long getStartTime()
  {
    if (ProjectForgeApp.getInstance() == null) {
      // Should only occur in test cases.
      return 0;
    }
    return ProjectForgeApp.getInstance().getStartTime();
  }

  @Value("${projectforge.wicket.developmentMode}")
  public void setDevelopmentMode(Boolean developmentMode)
  {
    WicketApplication.developmentMode = developmentMode;
  }

  /**
   * Own solution: uses development parameter of servlet context init parameter (see context.xml or server.xml).
   *
   * @return DEVELOPMENT, if development variable of servlet context is set to "true" otherwise DEPLOYMENT.
   * @see org.apache.wicket.protocol.http.WebApplication#getConfigurationType()
   */
  @Override
  public RuntimeConfigurationType getConfigurationType()
  {
    if (isDevelopmentSystem() == true) {
      return RuntimeConfigurationType.DEVELOPMENT;
    }
    return RuntimeConfigurationType.DEPLOYMENT;
  }

  private void addPluginResources()
  {
    for (AbstractPlugin plugin : pluginAdminService.getActivePlugin()) {
      final List<String> resourceBundleNames = plugin.getResourceBundleNames();
      resourceBundleNames.forEach(this::addResourceBundle);
    }
  }

  private void addResourceBundle(String bundleName)
  {
    // Prepend the resource bundle for overwriting some Wicket default localizations (such as StringValidator.*)
    getResourceSettings().getStringResourceLoaders().add(new BundleStringResourceLoader(bundleName));
    I18nHelper.addBundleName(bundleName);
  }

  @Override
  protected void init()
  {
    super.init();
    getComponentInstantiationListeners().add(
        new SpringComponentInjector(this, applicationContext));
    applicationContext.getBean(TenantsCache.class);
    projectForgeApp = ProjectForgeApp.init(applicationContext, isDevelopmentSystem());
    WebRegistry.getInstance().init();
    pluginAdminService.initializeActivePlugins();
    setDefaultPage(TeamCalCalendarPage.class);
    addResourceBundle(RESOURCE_BUNDLE_NAME);
    addPluginResources();
    getResourceSettings().getStringResourceLoaders().add(new ExternalResourceLoader());
    // Own error page for deployment mode and UserException and AccessException.
    getRequestCycleListeners().add(new AbstractRequestCycleListener()
    {
      /**
       * Log only non ProjectForge exceptions.
       *
       * @see org.apache.wicket.request.cycle.AbstractRequestCycleListener#onException(org.apache.wicket.request.cycle.RequestCycle,
       *      java.lang.Exception)
       */
      @Override
      public IRequestHandler onException(final RequestCycle cycle, final Exception ex)
      {
        // in case of expired session, please redirect to home page
        if (ex instanceof PageExpiredException) {
          return super.onException(cycle, ex);
        }

        // log StalePageException but do not redirect to error page
        if (ex instanceof StalePageException) {
          log.warn(ex);
          return super.onException(cycle, ex);
        }

        final Throwable rootCause = ExceptionHelper.getRootCause(ex);
        // log.error(rootCause.getMessage(), ex);
        // if (rootCause instanceof ProjectForgeException == false) {
        // return super.onException(cycle, ex);
        // }
        // return null;
        log.error(ex.getMessage(), ex);
        if (isDevelopmentSystem() == true) {

          if (rootCause instanceof SQLException) {
            SQLException next = (SQLException) rootCause;
            while ((next = next.getNextException()) != null) {
              log.error(next.getMessage(), next);
            }
          }
          return super.onException(cycle, ex);
        } else {
          // Show always this error page in production mode:
          return new RenderPageRequestHandler(new PageProvider(new ErrorPage(ex)));
        }
      }
    });

    getApplicationSettings().setDefaultMaximumUploadSize(Bytes.megabytes(100));
    getMarkupSettings().setDefaultMarkupEncoding("utf-8");
    final MyAuthorizationStrategy authStrategy = new MyAuthorizationStrategy();
    getSecuritySettings().setAuthorizationStrategy(authStrategy);
    getSecuritySettings().setUnauthorizedComponentInstantiationListener(authStrategy);

    if (isDevelopmentSystem() == false) {
      getResourceSettings().setThrowExceptionOnMissingResource(false); // Don't throw MissingResourceException for
      // missing i18n keys in production mode.
    }
    getApplicationSettings().setPageExpiredErrorPage(PageExpiredPage.class); // Don't show expired page.
    // getSessionSettings().setMaxPageMaps(20); // Map up to 20 pages per session (default is 5).
    getComponentInstantiationListeners().add(new SpringComponentInjector(this));
    getApplicationSettings().setInternalErrorPage(ErrorPage.class);
    getRequestCycleSettings().setTimeout(Duration.minutes(Const.WICKET_REQUEST_TIMEOUT_MINUTES));

    // getRequestCycleSettings().setGatherExtendedBrowserInfo(true); // For getting browser width and height.

    // Select2:
    // final ApplicationSettings select2Settings = ApplicationSettings.get();
    // select2Settings.setIncludeJavascript(false);

    // if ("true".equals(System.getProperty(SYSTEM_PROPERTY_HSQLDB_18_UPDATE)) == true) {
    // try {
    // log.info("Send SHUTDOWN COMPACT to upgrade data-base version:");
    // final DataSource dataSource = (DataSource)beanFactory.getBean("dataSource");
    // dataSource.getConnection().createStatement().execute("SHUTDOWN COMPACT");
    // log.fatal("************ PLEASE RESTART APPLICATION NOW FOR PROPER INSTALLATION !!!!!!!!!!!!!! ************");
    // return;
    // } catch (final SQLException ex) {
    // log.fatal("Data-base SHUTDOWN COMPACT failed: " + ex.getMessage());
    // }
    // }

    // Javascript Resource settings
    getJavaScriptLibrarySettings()
        .setJQueryReference(new PackageResourceReference(WicketApplication.class, "scripts/jquery.js"));

    final String configContextPath = configurationService.getServletContextPath();
    WicketUtils.setContextPath(configContextPath);

    for (final Map.Entry<String, Class<? extends WebPage>> mountPage : WebRegistry.getInstance().getMountPages()
        .entrySet()) {
      final String path = mountPage.getKey();
      final Class<? extends WebPage> pageClass = mountPage.getValue();
      mountPageWithPageParameterAwareness(path, pageClass);
      mountedPages.put(pageClass, path);
    }
    if (isDevelopmentSystem() == true) {
      if (isStripWicketTags() == true) {
        log.info("Strip Wicket tags also in development mode at default (see context.xml).");
        Application.get().getMarkupSettings().setStripWicketTags(true);
      }
      getDebugSettings().setOutputMarkupContainerClassName(true);
    }
    try {
      final UserContext internalSystemAdminUserContext = UserContext
          .__internalCreateWithSpecialUser(DatabaseUpdateService.__internalGetSystemAdminPseudoUser(),
              getUserGroupCache());
      ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
      if (databaseUpdater.getSystemUpdater().isUpdated() == false) {
        // Force redirection to update page:
        UserFilter.setUpdateRequiredFirst(true);
      }
    } finally {
      ThreadLocalUserContext.clear();
    }

    switch (loginService.getLoginHandlerClass()) {
      case "LdapMasterLoginHandler":
        loginHandler = applicationContext.getBean(LdapMasterLoginHandler.class);
        break;
      case "LdapSlaveLoginHandler":
        loginHandler = applicationContext.getBean(LdapSlaveLoginHandler.class);
        break;
      default:
        loginHandler = applicationContext.getBean(LoginDefaultHandler.class);
    }

    // initialize styles compiler
    try {
      final LessWicketApplicationInstantiator lessInstantiator = new LessWicketApplicationInstantiator(this, "styles",
          "projectforge.less", "projectforge.css", this.baseDir, configurationService.getCompileCss());
      lessInstantiator.instantiate();
    } catch (final Exception e) {
      log.error("Unable to instantiate wicket less compiler", e);
    }

    loginHandler.initialize();
    Login.getInstance().setLoginHandler(loginHandler);
    if (UserFilter.isUpdateRequiredFirst() == false) {
      projectForgeApp.finalizeInitialization();
    }

    getPageSettings().setRecreateMountedPagesAfterExpiry(false);
    initPageStore();
  }

  private TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  /**
   * Initializes the page store.
   */
  private void initPageStore()
  {
    //pf.configuration.web.cachedPagesPerSession", 10);
    getStoreSettings().setInmemoryCacheSize(10);

    // Set custom page store
    setPageManagerProvider(new DefaultPageManagerProvider(this)
    {
      @Override
      protected IPageStore newPageStore(IDataStore dataStore)
      {
        return new InMemoryPageStore();
      }
    });
  }

  private void mountPageWithPageParameterAwareness(final String path, final Class<? extends WebPage> pageClass)
  {
    mount(new PageParameterAwareMountedMapper(path, pageClass));
  }

  @Override
  protected void onDestroy()
  {
    ProjectForgeApp.shutdown();
  }

  /**
   * @return True if configured as servlet context param.
   */
  @Override
  public boolean isDevelopmentSystem()
  {
    if (developmentMode == null) {
      final String value = getServletContext().getInitParameter("development");
      developmentMode = "true".equals(value);
    }
    return developmentMode;
  }

  @Override
  public boolean isStripWicketTags()
  {
    if (stripWicketTags == null) {
      if (isDevelopmentSystem() == false) {
        stripWicketTags = true;
      } else {
        final String value = getServletContext().getInitParameter("stripWicketTags");
        stripWicketTags = "true".equals(value);
      }
    }
    return stripWicketTags;
  }

  /**
   * @see org.apache.wicket.Application#getHomePage()
   */
  @Override
  public Class<? extends WebPage> getHomePage()
  {
    return WicketUtils.getDefaultPage();
  }

  /**
   * From http://www.danwalmsley.com/2009/04/08/apache-wicket-on-google-app-engine-for-java/<br/>
   * Override the newSessionStore() method to return HttpSessionStore, because the default second level session store
   * uses java.io.File, which is sometimes not allowed.
   *
   * @see org.apache.wicket.Application#newSessionStore()
   */
  /*
   * @Override protected ISessionStore newSessionStore() { return new
   * org.apache.wicket.protocol.http.HttpSessionStore(this); }
   */
  @Override
  public Session newSession(final Request request, final Response response)
  {
    final MySession mySession = new MySession(request);
    return mySession;
  }

  /**
   *
   */
  @Override
  protected IConverterLocator newConverterLocator()
  {
    final ConverterLocator converterLocator = new ConverterLocator();
    converterLocator.set(java.util.Date.class, new MyDateConverter());
    converterLocator.set(java.sql.Date.class, new MyDateConverter(java.sql.Date.class, "S-"));
    return converterLocator;
  }

  //TODO: PROJECTFORGE-1715 - Hatte sonst Probleme beim Shutdown. Wenn wieder auftritt, dann Interface und diesen Code ausprobieren.
  // http://docs.spring.io/autorepo/docs/spring-framework/3.2.6.RELEASE/javadoc-api/org/springframework/context/SmartLifecycle.html
  //  private boolean isRunning;
  //
  //  @Override
  //  public void start()
  //  {
  //    log.info("WicketApplication Spring Bean start");
  //    isRunning = true;
  //  }
  //
  //  @Override
  //  public void stop()
  //  {
  //    log.info("WicketApplication Spring Bean stop");
  //    isRunning = false;
  //  }
  //
  //  @Override
  //  public boolean isRunning()
  //  {
  //    return isRunning;
  //  }
  //
  //  @Override
  //  public int getPhase()
  //  {
  //    return -1;
  //  }
  //
  //  @Override
  //  public boolean isAutoStartup()
  //  {
  //    return true;
  //  }
  //
  //  @Override
  //  public void stop(Runnable callback)
  //  {
  //    log.info("WicketApplication Spring Bean stop");
  //    isRunning = false;
  //  }
}
