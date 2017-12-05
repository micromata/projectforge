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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The controller of the edit formular page. Most functionality such as insert, update, delete etc. is done by the super
 * class.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingAttendeeEditPage extends
    AbstractEditPage<TrainingAttendeeDO, TrainingAttendeeEditForm, TrainingAttendeeDao> implements ISelectCallerPage
{

  private static final long serialVersionUID = 6051980938679738200L;

  private static final Logger log = LoggerFactory.getLogger(TrainingAttendeeEditPage.class);

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix.skilltraining.attendee";

  @SpringBean
  private TrainingAttendeeDao trainingAttendeeDao;

  /**
   * @param parameters
   */
  public TrainingAttendeeEditPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
    init();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected TrainingAttendeeDao getBaseDao()
  {
    return trainingAttendeeDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#newEditForm(org.projectforge.web.wicket.AbstractEditPage,
   *      org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected TrainingAttendeeEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage,
      final TrainingAttendeeDO data)
  {
    return new TrainingAttendeeEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {

  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {

  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {

  }

}
