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

package org.projectforge.web.fibu;

import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.user.service.UserPrefService;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.rest.core.AbstractPagesRest;
import org.projectforge.rest.core.PagesResolver;
import org.projectforge.rest.fibu.ProjectPagesRest;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.slf4j.Logger;

public class ProjektEditPage extends AbstractEditPage<ProjektDO, ProjektEditForm, ProjektDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 8763884579951937296L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjektEditPage.class);

  public ProjektEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.projekt");
    init();
  }

  @Override
  protected ProjektDao getBaseDao()
  {
    return WicketSupport.get(ProjektDao.class);
  }

  @Override
  protected ProjektEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final ProjektDO data)
  {
    return new ProjektEditForm(this, data);
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    var kost2Dao = WicketSupport.get(Kost2Dao.class);
    if (getData() != null && getData().getId() != null) {
      for (final Kost2Art art : form.kost2Arts) {
        if (art.isExistsAlready() == false && art.isSelected() == true) {
          final Kost2DO kost2 = new Kost2DO();
          kost2Dao.setProjekt(kost2, getData().getId());
          kost2Dao.setKost2Art(kost2, art.getId());
          kost2Dao.insert(kost2);
        }
      }
    }
    return null;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  public void setResponsePage() {
    if (returnToPageClass != null) {
      super.setResponsePage();
      return;
    }
    WicketSupport.get(UserPrefService.class).putEntry("project", AbstractPagesRest.USER_PREF_PARAM_HIGHLIGHT_ROW, getData().getId(), false);
    throw new RedirectToUrlException(PagesResolver.getListPageUrl(ProjectPagesRest.class, null, true));
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    var projektDao = WicketSupport.get(ProjektDao.class);
    if ("kundeId".equals(property) == true) {
      projektDao.setKunde(getData(), (Long) selectedValue);
      form.kundeSelectPanel.getTextField().modelChanged();
    } else if ("taskId".equals(property) == true) {
      projektDao.setTask(getData(), (Long) selectedValue);
    } else if ("projektManagerGroupId".equals(property) == true) {
      projektDao.setProjektManagerGroup(getData(), (Long) selectedValue);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("kundeId".equals(property) == true) {
      getData().setKunde(null);
      form.kundeSelectPanel.getTextField().modelChanged();
    } else if ("taskId".equals(property) == true) {
      getData().setTask(null);
    } else if ("projektManagerGroupId".equals(property) == true) {
      getData().setProjektManagerGroup(null);
      form.groupSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for unselection.");
    }
  }

}
