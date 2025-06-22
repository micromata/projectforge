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

package org.projectforge.web.wicket;

import de.micromata.less.LessWicketApplicationInstantiator;
import de.micromata.wicket.request.mapper.PageParameterAwareMountedMapper;
import org.apache.wicket.*;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.resource.loader.BundleStringResourceLoader;
import org.apache.wicket.settings.DebugSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.Constants;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.plugins.core.AbstractPlugin;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.rest.sipgate.SipgateDirectCallService;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.WicketMenuBuilder;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.session.MySession;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.wicket.converter.MyDateConverter;
import org.projectforge.web.wicket.converter.MyLocalDateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start
 * class.
 */
@Controller
public class WicketApplication extends WebApplication implements WicketApplicationInterface/* , SmartLifecycle */ {
    public static final String RESOURCE_BUNDLE_NAME = Constants.RESOURCE_BUNDLE_NAME;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WicketApplication.class);

    private static final Map<Class<? extends Page>, String> mountedPages = new HashMap<>();

    public static Class<? extends WebPage> DEFAULT_PAGE = TaskTreePage.class;

    private static Boolean stripWicketTags;

    private static Boolean developmentMode;

    private static Boolean testsystemMode;

    private static String testsystemColor;

    private static final long startTime = System.currentTimeMillis();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PluginAdminService pluginAdminService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private ProjectForgeApp projectForgeApp; // Needed to be constructed first.

    @Autowired
    private WicketMenuBuilder wicketMenuBuilder; // Needed to be constructed first.

    @Value("${projectforge.base.dir}")
    private String baseDir;

    /**
     * Constructor
     */
    public WicketApplication() {
        super();
    }

    public static Boolean getTestsystemMode() {
        return testsystemMode;
    }

    @Value("${projectforge.testsystemMode}")
    public void setTestsystemMode(Boolean testsystemMode) {
        WicketApplication.testsystemMode = testsystemMode;
    }

    public static String getTestsystemColor() {
        return testsystemColor;
    }

    @Value("${projectforge.testsystemColor}")
    public void setTestsystemColor(String testsystemColor) {
        WicketApplication.testsystemColor = testsystemColor;
    }

    /**
     * At application start the flag developmentMode is perhaps not already set. If possible please use
     * {@link #isDevelopmentSystem()} instead.<br/>
     * Please use {@link WebConfiguration#isDevelopmentMode()}.
     */
    public static Boolean internalIsDevelopmentMode() {
        return developmentMode;
    }

    /**
     * Please don't use this method, use {@link WicketUtils#getDefaultPage()} instead.
     *
     * @return
     */
    public static Class<? extends WebPage> internalGetDefaultPage() {
        return DEFAULT_PAGE;
    }

    /**
     * Use this method only if you want to change the default page (if no other is defined in config.xml).
     *
     * @param defaultPage
     */
    public static void setDefaultPage(final Class<? extends WebPage> defaultPage) {
        DEFAULT_PAGE = defaultPage;
    }

    public static String getBookmarkableMountPath(final Class<? extends Page> pageClass) {
        return mountedPages.get(pageClass);
    }

    @Deprecated
    public static long getStartTime() {
        return startTime;
    }

    /**
     * Own solution: uses development parameter of servlet context init parameter (see context.xml or server.xml).
     *
     * @return DEVELOPMENT, if development variable of servlet context is set to "true" otherwise DEPLOYMENT.
     * @see org.apache.wicket.protocol.http.WebApplication#getConfigurationType()
     */
    @Override
    public RuntimeConfigurationType getConfigurationType() {
        //if (isDevelopmentSystem() == true) {
        //  return RuntimeConfigurationType.DEVELOPMENT;
        //}
        return RuntimeConfigurationType.DEPLOYMENT;
    }

    private void addPluginResources() {
        for (AbstractPlugin plugin : pluginAdminService.getActivePlugins()) {
            final List<String> resourceBundleNames = plugin.getResourceBundleNames();
            resourceBundleNames.forEach(this::addResourceBundle);
        }
    }

    private void addResourceBundle(String bundleName) {
        // Prepend the resource bundle for overwriting some Wicket default localizations (such as StringValidator.*)
        getResourceSettings().getStringResourceLoaders().add(new BundleStringResourceLoader(bundleName));
        I18nHelper.addBundleName(bundleName);
    }

    @Override
    protected void init() {
        super.init();
        getCspSettings().blocking().disabled(); // Configuring new Content Security Policy (CSP) settings.
        getComponentInstantiationListeners().add(new SpringComponentInjector(this, applicationContext));
        // Wicket workaround for not be able to proxy Kotlin base SpringBeans:
        WicketSupport.register(applicationContext);
        WebRegistry.getInstance().init();
        pluginAdminService.initializeActivePlugins();
        setDefaultPage(DEFAULT_PAGE);
        addResourceBundle(RESOURCE_BUNDLE_NAME);
        addPluginResources();
        getResourceSettings().getStringResourceLoaders().add(new ExternalResourceLoader());
        // Own error page for deployment mode and UserException and AccessException.
        getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public IRequestHandler onException(RequestCycle cycle, Exception ex) {
                Throwable rootCause = ExceptionHelper.getRootCause(ex);
                // log.error(rootCause.getMessage(), ex);
                // if (rootCause instanceof ProjectForgeException == false) {
                // return super.onException(cycle, ex);
                // }
                // return null;

                // Logging
                log.error(ex.getMessage(), ex);

                if (isDevelopmentSystem()) {
                    if (rootCause instanceof SQLException sqlException) {
                        while (sqlException != null) {
                            log.error("SQL Exception: ", sqlException);
                            sqlException = sqlException.getNextException();
                        }
                    }
                    // In Development Mode, let Wicket handle the exception
                    return null;
                } else {
                    // In Production Mode, redirect to an error page
                    return new RenderPageRequestHandler(new PageProvider(new ErrorPage(ex)));
                }
            }
        });
        // Set Cache-Control Header for all pages.
        getRequestCycleListeners().add(new IRequestCycleListener() {
            @Override
            public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler) {
                if (cycle.getResponse() instanceof WebResponse response) {
                    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                }
            }
        });
        getApplicationSettings().setDefaultMaximumUploadSize(Bytes.megabytes(100));
        getMarkupSettings().setDefaultMarkupEncoding("utf-8");
        final MyAuthorizationStrategy authStrategy = new MyAuthorizationStrategy();
        getSecuritySettings().setAuthorizationStrategy(authStrategy);
        getSecuritySettings().setUnauthorizedComponentInstantiationListener(authStrategy);

        if (!isDevelopmentSystem()) {
            getResourceSettings().setThrowExceptionOnMissingResource(false); // Don't throw MissingResourceException for
            // missing i18n keys in production mode.
        }
        getApplicationSettings().setPageExpiredErrorPage(PageExpiredPage.class); // Don't show expired page.
        // getSessionSettings().setMaxPageMaps(20); // Map up to 20 pages per session (default is 5).
        getApplicationSettings().setInternalErrorPage(ErrorPage.class);
        getRequestCycleSettings().setTimeout(Duration.ofMinutes(Constants.WICKET_REQUEST_TIMEOUT_MINUTES));
        // getRequestCycleSettings().setRenderStrategy(RequestCycleSettings.RenderStrategy.ONE_PASS_RENDER);

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

        final String configContextPath = domainService.getContextPath();
        WicketUtils.setContextPath(configContextPath);

        for (final Map.Entry<String, Class<? extends WebPage>> mountPage : WebRegistry.getInstance().getMountPages()
                .entrySet()) {
            final String path = mountPage.getKey();
            final Class<? extends WebPage> pageClass = mountPage.getValue();
            mountPageWithPageParameterAwareness(path, pageClass);
            mountedPages.put(pageClass, path);
        }
        if (isDevelopmentSystem()) {
            if (isStripWicketTags()) {
                log.info("Strip Wicket tags also in development mode at default (see context.xml).");
                Application.get().getMarkupSettings().setStripWicketTags(true);
            }
            getDebugSettings().setOutputMarkupContainerClassNameStrategy(DebugSettings.ClassOutputStrategy.HTML_COMMENT);

            // For getting more information of deserialization issues: use jvm parameter --add-opens java.base/java.io=ALL-UNNAMED
        }
        try {
            final UserContext internalSystemAdminUserContext = UserContext
                    .__internalCreateWithSpecialUser(DatabaseService.__internalGetSystemAdminPseudoUser());
            ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
        } finally {
            ThreadLocalUserContext.clear();
        }

        // initialize styles compiler
        try {
            final LessWicketApplicationInstantiator lessInstantiator = new LessWicketApplicationInstantiator(this, "styles",
                    "projectforge.less", "projectforge.css", this.baseDir, configurationService.getCompileCss());
            lessInstantiator.instantiate();
        } catch (final Exception e) {
            log.error("Unable to instantiate wicket less compiler: " + e.getMessage(), e);
        }

        getPageSettings().setRecreateBookmarkablePagesAfterExpiry(true);
        getPageSettings().setVersionPagesByDefault(true);

        // Configure standard Wicket settings to fix session-mixing issues after Wicket 10.4 migration

        // These settings help with preserving page state and isolation between users
        getStoreSettings().setAsynchronous(false);
        getStoreSettings().setMaxSizePerSession(Bytes.megabytes(5));
        log.info("Using standard Wicket disk-based page store to fix session problems after Wicket 10 migration");

        WicketSupport.register(SipgateDirectCallService.class, applicationContext.getBean(SipgateDirectCallService.class));

    }

    private void mountPageWithPageParameterAwareness(final String path, final Class<? extends WebPage> pageClass) {
        mount(new PageParameterAwareMountedMapper(path, pageClass));
    }

    /**
     * @return True if configured as servlet context param.
     */
    @Override
    public boolean isDevelopmentSystem() {
        if (developmentMode == null) {
            final String value = getServletContext().getInitParameter("development");
            developmentMode = "true".equals(value);
        }
        return developmentMode;
    }

    @Override
    public boolean isStripWicketTags() {
        if (stripWicketTags == null) {
            if (!isDevelopmentSystem()) {
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
    public Class<? extends WebPage> getHomePage() {
        return WicketUtils.getDefaultPage();
    }

    /**
     * From http://www.danwalmsley.com/2009/04/08/apache-wicket-on-google-app-engine-for-java/<br/>
     * Override the newSessionStore() method to return HttpSessionStore, because the default second level session store
     * uses java.io.File, which is sometimes not allowed.
     */
    /*
     * @Override protected ISessionStore newSessionStore() { return new
     * org.apache.wicket.protocol.http.HttpSessionStore(this); }
     */
    @Override
    public Session newSession(final Request request, final Response response) {
        final MySession mySession = new MySession(request);
        return mySession;
    }

    /**
     *
     */
    @Override
    protected IConverterLocator newConverterLocator() {
        final ConverterLocator converterLocator = new ConverterLocator();
        converterLocator.set(LocalDate.class, new MyLocalDateConverter());
        converterLocator.set(java.util.Date.class, new MyDateConverter());
        converterLocator.set(java.sql.Date.class, new MyDateConverter(java.sql.Date.class, "S-"));
        return converterLocator;
    }

}
