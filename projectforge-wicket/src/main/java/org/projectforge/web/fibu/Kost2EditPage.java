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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;


@EditPage(defaultReturnPage = Kost2ListPage.class)
public class Kost2EditPage extends AbstractEditPage<Kost2DO, Kost2EditForm, Kost2Dao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 1622932678249306556L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Kost2EditPage.class);

  public Kost2EditPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kost2");
    init();
  }

  @Override
  protected Kost2Dao getBaseDao()
  {
    return WicketSupport.get(Kost2Dao.class);
  }

  @Override
  protected Kost2EditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final Kost2DO data)
  {
    return new Kost2EditForm(this, data);
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("projektId".equals(property) == true) {
      WicketSupport.get(Kost2Dao.class).setProjekt(getData(), (Long) selectedValue);
      form.projektSelectPanel.getTextField().modelChanged();
      form.nummernkreisField.modelChanged();
      form.bereichField.modelChanged();
      form.teilbereichField.modelChanged();
      form.kost2ArtField.modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("projektId".equals(property) == true) {
      getData().setProjekt(null);
      form.projektSelectPanel.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
