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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.persistence.jpa.impl.LuceneServiceImpl;
import org.projectforge.framework.utils.ExceptionHelper;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.AbstractStandardFormPage;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class LuceneConsolePage extends AbstractStandardFormPage
{
  private static final long serialVersionUID = -8866862318651809124L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LuceneConsolePage.class);

  @SpringBean
  LuceneServiceImpl luceneService;

  @SpringBean
  private ConfigurationService configurationService;

  private final LuceneConsoleForm form;

  public LuceneConsolePage(final PageParameters parameters)
  {
    super(parameters);
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    log.warn("Lucene console is called by the user!");
    final SecurityConfig securityConfig = configurationService.getSecurityConfig();
    final boolean sqlConsoleAvailable = WebConfiguration.isDevelopmentMode() == true
        || configurationService.isSqlConsoleAvailable() == true
        || (securityConfig != null && securityConfig.isSqlConsoleAvailable() == true);
    if (sqlConsoleAvailable == false) {
      throw new AccessException("access.exception.violation",
          "The Lucene console isn't available (isn't configured). May-be this is an attack!");
    }
    form = new LuceneConsoleForm(this, luceneService);
    body.add(form);
    form.init();
  }

  void excecuteLucene(boolean lucene, Class<?> entityClass, final String sql, String fieldList)
  {
    checkAccess();
    log.info("Executing Lucene: " + sql);
    try {
      String resultString;

      if (lucene == true) {
        resultString = luceneService.searchSimple(entityClass, sql, fieldList);
      } else {
        resultString = luceneService.searchViaHibernateSearch(entityClass, sql, fieldList);
      }
      form.setResultString(resultString);
    } catch (final Exception ex) {
      log.info("Lucene statement produced an error: " + ex.getMessage());
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
