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

package org.projectforge.web.admin;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService.RestoreMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.database.PfJpaXmlDumpService;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.web.LoginPage;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.AbstractUnsecureBasePage;
import org.projectforge.web.wicket.MessagePage;
import org.projectforge.web.wicket.WicketUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class SetupPage extends AbstractUnsecureBasePage
{
  private static final long serialVersionUID = 9174903871130640690L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SetupPage.class);

  @SpringBean
  private ConfigurationDao configurationDao;

  @SpringBean
  private HibernateSearchReindexer hibernateSearchReindexer;

  @SpringBean
  private DatabaseService databaseService;

  @SpringBean
  private PfJpaXmlDumpService jpaXmlDumpService;

  private final SetupForm setupForm;

  private final SetupImportForm importForm;

  @SpringBean
  private MenuCreator menuCreator;

  public SetupPage(final PageParameters parameters)
  {
    super(parameters);
    checkAccess();
    setupForm = new SetupForm(this);
    body.add(setupForm);
    setupForm.init();
    importForm = new SetupImportForm(this);
    body.add(importForm);
    importForm.init();
  }

  protected void finishSetup()
  {
    log.info("Finishing the set-up...");
    checkAccess();
    PFUserDO adminUser = setupForm.getAdminUser();
    final String message;

    //Init default tenant
    databaseService.insertDefaultTenant();
    //Init global addressbook
    databaseService.insertGlobalAddressbook();

    if (setupForm.getSetupMode() == SetupTarget.EMPTY_DATABASE) {
      //Init default data (admin user, groups and root task)
      databaseService.initializeDefaultData(adminUser, setupForm.getTimeZone());
      message = "administration.setup.message.emptyDatabase";
    } else {
      try {
        ScriptUtils.executeSqlScript(databaseService.getDataSource().getConnection(),
            configurationDao.getApplicationContext().getResource("classpath:data/pfTestdata.sql"));
        if (databaseService.getDialect() == DatabaseDialect.PostgreSQL) {
          ScriptUtils.executeSqlScript(databaseService.getDataSource().getConnection(),
                  configurationDao.getApplicationContext().getResource("classpath:data/pfTestdataPostgres.sql"));
        }
      } catch (Exception e) {
        log.error("Exception occured while running test data insert script. Message: " + e.getMessage());
      }
      GlobalConfiguration.getInstance().forceReload();
      adminUser = databaseService.updateAdminUser(adminUser, setupForm.getTimeZone());
      databaseService.afterCreatedTestDb(false);
      message = "administration.setup.message.testdata";
      // refreshes the visibility of the costConfigured dependent menu items:
      menuCreator.refresh();
    }

    loginAdminUser(adminUser);

    configurationDao.checkAndUpdateDatabaseEntries();
    if (setupForm.getTimeZone() != null) {
      final ConfigurationDO configurationDO = getConfigurationDO(ConfigurationParam.DEFAULT_TIMEZONE);
      if (configurationDO != null) {
        configurationDO.setTimeZone(setupForm.getTimeZone());
        configurationDao.update(configurationDO);
      }
    }
    configure(ConfigurationParam.CALENDAR_DOMAIN, setupForm.getCalendarDomain());
    configure(ConfigurationParam.SYSTEM_ADMIN_E_MAIL, setupForm.getSysopEMail());
    configure(ConfigurationParam.FEEDBACK_E_MAIL, setupForm.getFeedbackEMail());
    if (databaseService.getSystemUpdater().isUpdated() == true) {
      // Update status:
      UserFilter.setUpdateRequiredFirst(false);
    }
    setResponsePage(new MessagePage(message, adminUser.getUsername()));
    log.info("Set-up finished.");
  }

  private void loginAdminUser(PFUserDO adminUser)
  {
    //Login admin user
    final UserContext userContext = new UserContext(adminUser, getUserGroupCache());
    ((MySession) getSession()).login(userContext, getRequest());
    UserFilter.login(WicketUtils.getHttpServletRequest(getRequest()), userContext);
  }

  private ConfigurationDO getConfigurationDO(final ConfigurationParam param)
  {
    final ConfigurationDO configurationDO = configurationDao.getEntry(param);
    if (configurationDO == null) {
      log.error("Oups, can't find configuration parameter '" + param + "'. You can re-configure it anytime later.");
    }
    return configurationDO;
  }

  private void configure(final ConfigurationParam param, final String value)
  {
    if (StringUtils.isBlank(value) == true) {
      return;
    }
    final ConfigurationDO configurationDO = getConfigurationDO(param);
    if (configurationDO != null) {
      configurationDO.setStringValue(value);
      configurationDao.update(configurationDO);
    }
  }

  protected void upload()
  {
    checkAccess();
    log.info("Uploading data-base dump file...");
    final FileUpload fileUpload = importForm.fileUploadField.getFileUpload();
    if (fileUpload == null) {
      return;
    }
    try {
      final String clientFileName = fileUpload.getClientFileName();
      InputStream is = null;
      if (clientFileName.endsWith(".xml.gz") == true) {
        is = new GZIPInputStream(fileUpload.getInputStream());
      } else if (clientFileName.endsWith(".xml") == true) {
        is = fileUpload.getInputStream();
      } else {
        log.info("Unsupported file suffix. Only *.xml and *.xml.gz is supported: " + clientFileName);
        error(getString("administration.setup.error.uploadfile"));
        return;
      }
      //      final XStreamSavingConverter converter = xmlDump.restoreDatabase(reader);
      //      final int counter = xmlDump.verifyDump(converter);
      //      configurationDao.checkAndUpdateDatabaseEntries();

      // intialize DB schema
      this.databaseService.updateSchema();

      int counter = jpaXmlDumpService.restoreDb(PfEmgrFactory.get(), is, RestoreMode.InsertAll);
      Configuration.getInstance().setExpired();
      final TaskTree taskTree = TaskTreeHelper.getTaskTree();
      taskTree.setExpired();
      getUserGroupCache().setExpired();
      new Thread()
      {
        @Override
        public void run()
        {
          hibernateSearchReindexer.rebuildDatabaseSearchIndices();
        }
      }.start();
      if (counter > 0) {
        ((MySession) getSession()).logout();
        setResponsePage(LoginPage.class);
      } else {
        error(getString("administration.setup.error.import"));
      }
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
      error(getString("administration.setup.error.import"));
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("administration.setup.title");
  }

  private void checkAccess()
  {
    if (databaseService.databaseTablesWithEntriesExists() == true) {
      log.error("Couldn't call set-up page, because the data-base isn't empty!");
      ((MySession) getSession()).logout();
      throw new RestartResponseException(WicketUtils.getDefaultPage());
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#thisIsAnUnsecuredPage()
   */
  @Override
  protected void thisIsAnUnsecuredPage()
  {
  }
}
