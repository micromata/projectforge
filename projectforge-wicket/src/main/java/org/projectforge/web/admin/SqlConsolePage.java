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

import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.continuousdb.DatabaseResultRow;
import org.projectforge.continuousdb.DatabaseResultRowEntry;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class SqlConsolePage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -8866862318651809124L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SqlConsolePage.class);

  @SpringBean
  private DatabaseService myDatabaseUpdater;

  @SpringBean
  private ConfigurationService configurationService;

  private final SqlConsoleForm form;

  public SqlConsolePage(final PageParameters parameters)
  {
    super(parameters);
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    log.warn("SQL console is called by the user!");
    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    final boolean sqlConsoleAvailable = WebConfiguration.isDevelopmentMode() == true
        || configurationService.isSqlConsoleAvailable() == true
        || (securityConfig != null && securityConfig.isSqlConsoleAvailable() == true);
    if (sqlConsoleAvailable == false) {
      throw new AccessException("access.exception.violation",
          "The SQL console isn't available (isn't configured). May-be this is an attack!");
    }
    form = new SqlConsoleForm(this);
    body.add(form);
    form.init();
  }

  void excecute(final String sql)
  {
    checkAccess();
    log.info("Executing sql: " + sql);
    try {
      final StringBuilder sb = new StringBuilder();
      if (sql.trim().toLowerCase().startsWith("select") == true) {
        final List<DatabaseResultRow> result = myDatabaseUpdater.query(sql);
        if (result != null && result.size() > 0) {
          final DatabaseResultRow firstRow = result.get(0);
          final List<DatabaseResultRowEntry> entries = firstRow.getEntries();
          if (entries != null && entries.size() > 0) {
            sb.append("<table>");
            sb.append("<tr>");
            for (final DatabaseResultRowEntry entry : entries) {
              sb.append("<th>").append(HtmlHelper.escapeHtml(entry.getName(), false)).append("</th>");
            }
            sb.append("</tr>");
            for (final DatabaseResultRow row : result) {
              sb.append("<tr>");
              for (final DatabaseResultRowEntry entry : row.getEntries()) {
                sb.append("<td>").append(HtmlHelper.escapeHtml(String.valueOf(entry.getValue()), true)).append("</td>");
              }
              sb.append("</tr>");
            }
            sb.append("</table>");
          }
        }
        form.setResultString(sb.toString());
      } else {
        myDatabaseUpdater.execute(sql);
        form.setResultString("Statement executed. See log files for further information.");
      }
    } catch (final Exception ex) {
      log.info("SQL statement produced an error: " + ex.getMessage());
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
