/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The controller of the edit formular page. Most functionality such as insert, update, delete etc. is done by the super
 * class.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingEditPage extends AbstractEditPage<TrainingDO, TrainingEditForm, TrainingDao>
    implements ISelectCallerPage
{

  private static final long serialVersionUID = 2710329392704763921L;

  private static final Logger log = LoggerFactory.getLogger(TrainingEditPage.class);

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix.skilltraining";

  public static final String PARAM_PARENT_SKILL_ID = "parentSkillId";

  @SpringBean
  private TrainingDao trainingDao;

  /**
   * @param parameters
   */
  public TrainingEditPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
    init();
    addTopMenuPanel();
    final Integer parentSkillId = WicketUtils.getAsInteger(parameters, PARAM_PARENT_SKILL_ID);
    if (NumberHelper.greaterZero(parentSkillId)) {
      trainingDao.setSkill(getData(), parentSkillId);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected TrainingDao getBaseDao()
  {
    return trainingDao;
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
  protected TrainingEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TrainingDO data)
  {
    return new TrainingEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("skillId".equals(property)) {
      trainingDao.setSkill(getData(), (Integer) selectedValue);
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
    if ("skillId".equals(property)) {
      trainingDao.setSkill(getData(), null);
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
    // Do nothing
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    trainingDao.setFullAccessGroups(getData(), form.fullAccessGroupsListHelper.getAssignedItems());
    trainingDao.setReadOnlyAccessGroups(getData(), form.readonlyAccessGroupsListHelper.getAssignedItems());
    return super.onSaveOrUpdate();
  }

  private void addTopMenuPanel()
  {
    if (!isNew()) {
      final Integer id = form.getData().getId();

      @SuppressWarnings("serial")
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final PageParameters params = new PageParameters();
              params.set(TrainingEditForm.PARAM_TRAINING_ID, id);
              final TrainingAttendeeListPage page = new TrainingAttendeeListPage(params);
              page.setReturnToPage(TrainingEditPage.this);
              setResponsePage(page);
            }
          }, getString("plugins.skillmatrix.skilltraining.attendee.menu"));
      addContentMenuEntry(menu);
    }
  }

}
