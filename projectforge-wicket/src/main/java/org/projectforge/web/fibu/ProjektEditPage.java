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

package org.projectforge.web.fibu;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = ProjektListPage.class)
public class ProjektEditPage extends AbstractEditPage<ProjektDO, ProjektEditForm, ProjektDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 8763884579951937296L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjektEditPage.class);

  @SpringBean
  private Kost2Dao kost2Dao;

  @SpringBean
  private ProjektDao projektDao;

  public ProjektEditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.projekt");
    init();
  }

  @Override
  protected ProjektDao getBaseDao()
  {
    return projektDao;
  }

  @Override
  protected ProjektEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final ProjektDO data)
  {
    return new ProjektEditForm(this, data);
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    if (getData() != null && getData().getId() != null) {
      for (final Kost2Art art : form.kost2Arts) {
        if (art.isExistsAlready() == false && art.isSelected() == true) {
          final Kost2DO kost2 = new Kost2DO();
          kost2Dao.setProjekt(kost2, getData().getId());
          kost2Dao.setKost2Art(kost2, art.getId());
          kost2Dao.save(kost2);
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
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("kundeId".equals(property) == true) {
      projektDao.setKunde(getData(), (Integer) selectedValue);
      form.kundeSelectPanel.getTextField().modelChanged();
    } else if ("taskId".equals(property) == true) {
      projektDao.setTask(getData(), (Integer) selectedValue);
    } else if ("projektManagerGroupId".equals(property) == true) {
      projektDao.setProjektManagerGroup(getData(), (Integer) selectedValue);
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
