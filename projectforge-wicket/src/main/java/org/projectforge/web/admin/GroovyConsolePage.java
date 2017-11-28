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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.AbstractStandardFormPage;

import groovy.lang.GroovyShell;

public class GroovyConsolePage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -8866862318651809124L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroovyConsolePage.class);

  @SpringBean
  private DatabaseService myDatabaseUpdater;

  @SpringBean
  private ConfigurationService configurationService;

  private final GroovyConsoleForm form;

  public GroovyConsolePage(final PageParameters parameters)
  {
    super(parameters);
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    log.warn("Groovy console is called by the user!");
    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    final boolean sqlConsoleAvailable = WebConfiguration.isDevelopmentMode() == true
        || configurationService.isSqlConsoleAvailable() == true
        || (securityConfig != null && securityConfig.isSqlConsoleAvailable() == true);
    if (sqlConsoleAvailable == false) {
      throw new AccessException("access.exception.violation",
          "The Groovy console isn't available (isn't configured). May-be this is an attack!");
    }
    form = new GroovyConsoleForm(this);
    body.add(form);
    form.init();
  }

  void excecute(final String sql)
  {
    checkAccess();
    log.info("Executing groovy: " + sql);
    try {

      Map<String, Object> context = new HashMap<>();

      StringWriter sout = new StringWriter();
      PrintWriter pout = new PrintWriter(sout);
      context.put("out", pout);
      GroovyShell shell = new GroovyShell(Thread.currentThread().getContextClassLoader());
      for (Map.Entry<String, Object> me : context.entrySet()) {
        shell.setVariable(me.getKey(), me.getValue());
      }
      shell.evaluate(sql);
      pout.flush();
      sout.flush();
      form.setResultString(HtmlHelper.escapeHtml(sout.getBuffer().toString(), true));
    } catch (final Exception ex) {
      log.info("Groovy  statement produced an error: " + ex.getMessage());
      form.setResultString(HtmlHelper.escapeHtml(ExceptionHelper.printStackTrace(ex), true));
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("system.admin.title");
  }

  private void checkAccess()
  {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    accessChecker.checkRestrictedOrDemoUser();
  }
}
