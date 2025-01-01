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

package org.projectforge;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.fortuna.ical4j.util.CompatibilityHints;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.system.SystemInfoCache;
import org.projectforge.common.CanonicalFileUtils;
import org.projectforge.common.EmphasizedLogSupport;
import org.projectforge.common.StringModifier;
import org.projectforge.database.DatabaseSupport;
import org.projectforge.export.MyXlsExportContext;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.PFSpringConfiguration;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.jcr.RepoBackupService;
import org.projectforge.jcr.RepoService;
import org.projectforge.registry.Registry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

/**
 * Doing some initialization stuff and stuff on shutdown (planned). Most stuff is yet done by SrpingApplication.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class ProjectForgeApp {
    public static final String CLASSPATH_INITIAL_BASEDIR_FILES = "initialBaseDirFiles";

    public static final String CONFIG_PARAM_BASE_DIR = "projectforge.base.dir";

    /**
     * "postgres" for setup wizard without embedded db option (useful for docker-compose with PostgreSQL) or null.
     */
    public static final String PROJECTFORGE_SETUP = "projectforge.setup";

    /**
     * If given, ProjectForge runs in docker. Values are: stack (for docker compose or single).
     */
    public static final String DOCKER_MODE = "docker";

    public static final String CONFIG_PLUGINS_DIR = "projectforge.plugins.dir";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApp.class);

    private static boolean junitTestMode = false;

    /**
     * Set by AbstractTestBase for avoiding the creation of config files (projectforge.properties, config.xml and attrschema.xml).
     */
    public static void internalSetJunitTestMode() {
        junitTestMode = true;
    }

    private static ConfigurableApplicationContext springApplicationRunContext;

    private static Class<?> springApplication;

    private boolean upAndRunning;

    private final long startTime = System.currentTimeMillis();

    private ApplicationContext applicationContext;

    private DatabaseService databaseService;

    private DomainService domainService;

    private Environment environment;

    private RepoService repoService;

    private RepoBackupService repoBackupService;

    private ShutdownService shutdownService;

    private SystemInfoCache systemInfoCache;

    private SystemStatus systemStatus;

    /**
     * Will be called by SpringApplication main class (ProjectForgeApplication).
     *
     * @param ctx The context returned by run.
     */
    public static void setSpringApplicationRunContext(ConfigurableApplicationContext ctx, Class<?> springApplicationClass) {
        springApplicationRunContext = ctx;
        springApplication = springApplicationClass;
    }

    @Autowired
    ProjectForgeApp(ApplicationContext applicationContext,
                    DatabaseService databaseService,
                    DomainService domainService,
                    Environment environment,
                    RepoService repoService,
                    RepoBackupService repoBackupService,
                    ShutdownService shutdownService,
                    SystemInfoCache systemInfoCache,
                    SystemStatus systemStatus) {
        this.applicationContext = applicationContext;
        this.databaseService = databaseService;
        this.domainService = domainService;
        this.environment = environment;
        this.repoService = repoService;
        this.repoBackupService = repoBackupService;
        this.shutdownService = shutdownService;
        this.systemInfoCache = systemInfoCache;
        this.systemStatus = systemStatus;
    }

    @PostConstruct
    void postConstruct() {
        Registry.getInstance().init(applicationContext);
        if (!junitTestMode) { // On test cases on repo will be used, if any.
            repoService.init(new File(ConfigXml.getInstance().getJcrDirectory()));
        }
        repoBackupService.initBackupDir(new File(ConfigXml.getInstance().getBackupDirectory()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startApp() {
        internalInit();
        finalizeInitialization();
        PFSpringConfiguration.logCorsFilterWarning(log);
    }

    @PreDestroy
    public void shutdownApp() {
        internalShutdown();
    }

    /**
     * Should be called on start-up (e. g. by WicketApplication) if all start-up stuff is done and all the services and
     * login should be started. <br>
     * Flag upAndRunning will be set to true.
     */
    private void finalizeInitialization() {
        log.info(ProjectForgeVersion.APP_ID + " " + ProjectForgeVersion.VERSION_NUMBER + " initialized.");
        // initialize ical4j to be more "relaxed"
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
        this.upAndRunning = true;
        systemStatus.setUpAndRunning(true);
        new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL)
                .log("ProjectForge is now available (up and running): localhost:" + environment.getProperty("server.port"))
                .log("Configured domain: " + domainService.getDomainWithContextPath())
                .logEnd();
    }

    private void internalInit() {
        log.info("Initializing...");
        // Time zone
        log.info("Default TimeZone is: " + TimeZone.getDefault());
        if (!"UTC".equals(TimeZone.getDefault().getID())) {
            new EmphasizedLogSupport(log, EmphasizedLogSupport.Alignment.LEFT)
                    .log("It's highly recommended to start ProjectForge with TimeZone UTC. This default TimeZone has to be")
                    .log("set before any initialization of Hibernate!!!! You may do this through JAVA_OPTS etc.")
                    .logEnd();
        }
        log.info("user.timezone is: " + System.getProperty("user.timezone"));

        // Initialize Excel extensions:
        new MyXlsExportContext();

        // HIBERNATE5
        //    HibernateUtils.setConfiguration(hibernateConfiguration);

        if (DatabaseSupport.getInstance() == null) {
            DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDatabaseDialect()));
        }

        SystemInfoCache.internalInitialize(systemInfoCache);
    }

    private void internalShutdown() {
        log.info("Shutdown...");
        shutdownService.shutdown();
        upAndRunning = false;
        try {
            final UserContext internalSystemAdminUserContext = UserContext
                    .__internalCreateWithSpecialUser(DatabaseService.__internalGetSystemAdminPseudoUser());
            ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
            databaseService.shutdownDatabase();
        } finally {
            ThreadLocalUserContext.clear();
        }
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        log.info("Cleaning up tmp directory: " + tmpDir.getAbsolutePath());
        File[] files = tmpDir.listFiles();
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory() && name.startsWith("tomcat.") || name.startsWith("projectforge-application")) {
                try {
                    FileUtils.deleteDirectory(file);
                    log.info("Deleting tmp directory: " + file.getAbsolutePath());
                } catch (IOException ex) {
                    log.error("Error while deleting tmp directory '" + file.getAbsolutePath() + "': " + ex.getMessage(), ex);
                }
            }
        }
        log.info("Shutdown completed.");
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return the upAndRunning
     */
    public boolean isUpAndRunning() {
        return upAndRunning;
    }

    /**
     * This method should only be called in test cases!
     *
     * @param upAndRunning the upAndRunning to set
     */
    public void internalSetUpAndRunning(final boolean upAndRunning) {
        log.warn("This method should only be called in test cases!");
        this.upAndRunning = upAndRunning;
    }

    /**
     * @return True, if the dest file exists or was created successfully. False if an error while creation occurred.
     */
    public static boolean ensureInitialConfigFile(String classPathSourceFilename, String destFilename) {
        String baseDir = System.getProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR);
        return ensureInitialConfigFile(new File(baseDir), classPathSourceFilename, destFilename, true, null);
    }

    /**
     * @return True, if the dest file exists or was created successfully. False if an error while creation occurred.
     */
    public static boolean ensureInitialConfigFile(File baseDir, String classPathSourceFilename, String destFilename, boolean logEnabled, StringModifier modifier) {
        if (junitTestMode)
            return true;
        final File destFile = new File(baseDir, destFilename);
        if (!destFile.exists()) {
            if (logEnabled)
                log.info("New installation, creating default '" + destFilename + "': " + CanonicalFileUtils.absolutePath(destFile));
            try {
                String resourcePath = ProjectForgeApp.CLASSPATH_INITIAL_BASEDIR_FILES + "/" + classPathSourceFilename;
                InputStream resourceStream = ProjectForgeApp.class.getClassLoader().getResourceAsStream(resourcePath);
                if (resourceStream == null) {
                    log.error("Internal error: Can't read initial config data from class path: " + resourcePath);
                    return false;
                }
                String content = IOUtils.toString(resourceStream, "UTF-8");
                if (modifier != null) {
                    content = modifier.modify(content);
                }
                FileUtils.writeStringToFile(destFile, content, "UTF-8", false);
            } catch (IOException ex) {
                log.error("Error while creating ProjectForge's config file '" + CanonicalFileUtils.absolutePath(destFile) + "': " + ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }
}
