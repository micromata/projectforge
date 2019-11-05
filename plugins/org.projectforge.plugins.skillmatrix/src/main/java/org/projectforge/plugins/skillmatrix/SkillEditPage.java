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
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillEditPage extends AbstractEditPage<SkillDO, SkillEditForm, SkillDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 4317454400876214258L;

  private static final Logger log = LoggerFactory.getLogger(SkillEditPage.class);

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix.skill";

  public static final String PARAM_PARENT_SKILL_ID = "parentSkillId";

  @SpringBean
  private SkillDao skillDao;

  @SpringBean
  private UserDao userDao;

  public SkillEditPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
    init();
    addTopMenuPanel();
    final Integer parentSkillId = WicketUtils.getAsInteger(parameters, PARAM_PARENT_SKILL_ID);
    if (NumberHelper.greaterZero(parentSkillId)) {
      skillDao.setParentSkill(getData(), parentSkillId);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected SkillDao getBaseDao()
  {
    return skillDao;
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
  protected SkillEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final SkillDO data)
  {
    return new SkillEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("parentId".equals(property)) {
      skillDao.setParentSkill(getData(), (Integer) selectedValue);
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
    if ("parentId".equals(property)) {
      getData().setParent(null);
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
    skillDao.setFullAccessGroups(getData(), form.fullAccessGroupsListHelper.getAssignedItems());
    skillDao.setReadonlyAccessGroups(getData(), form.readOnlyAccessGroupsListHelper.getAssignedItems());
    skillDao.setTrainingAccessGroups(getData(), form.trainingAccessGroupsListHelper.getAssignedItems());
    return super.onSaveOrUpdate();
  }

  @SuppressWarnings("serial")
  private void addTopMenuPanel()
  {
    if (!isNew()) {

      final Integer[] curUserGroupIds = userDao.getAssignedGroups(ThreadLocalUserContext.getUser())
          .toArray(new Integer[0]);
      final Integer id = form.getData().getId();

      final Integer[] fullAccessGroupIds = form.skillRight.getFullAccessGroupIds(getData());
      boolean isUserInFullAccessGroup = false;
      fullAccessLoop: for (final Integer i : curUserGroupIds) {
        for (final Integer j : fullAccessGroupIds) {
          if (i == j) {
            isUserInFullAccessGroup = true;
            break fullAccessLoop;
          }
        }
      }

      if (isUserInFullAccessGroup) {
        final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            new Link<Void>(ContentMenuEntryPanel.LINK_ID)
            {
              @Override
              public void onClick()
              {
                final PageParameters params = new PageParameters();
                params.set(PARAM_PARENT_SKILL_ID, id);
                final SkillEditPage skillEditPage = new SkillEditPage(params);
                skillEditPage.setReturnToPage(SkillEditPage.this);
                setResponsePage(skillEditPage);
              }
            }, getString("plugins.skillmatrix.skill.menu.addSubSkill"));
        addContentMenuEntry(menu);
      }

      final Integer[] trainingGroupIds = form.skillRight.getTrainingAccessGroupIds(getData());
      boolean isUserInTrainingGroup = false;
      trainingLoop: for (final Integer i : curUserGroupIds) {
        for (final Integer j : trainingGroupIds) {
          if (i == j) {
            isUserInTrainingGroup = true;
            break trainingLoop;
          }
        }
      }

      if (isUserInTrainingGroup) {
        final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            new Link<Void>(ContentMenuEntryPanel.LINK_ID)
            {
              @Override
              public void onClick()
              {
                final PageParameters params = new PageParameters();
                params.add(TrainingEditPage.PARAM_PARENT_SKILL_ID, id);
                final TrainingEditPage trainingEditPage = new TrainingEditPage(params);
                trainingEditPage.setReturnToPage(SkillEditPage.this);
                setResponsePage(trainingEditPage);
              }
            }, getString("plugins.skillmatrix.skilltraining.menu"));
        addContentMenuEntry(menu);
      }

      if (isUserInFullAccessGroup) {
        final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            new Link<Void>(ContentMenuEntryPanel.LINK_ID)
            {
              @Override
              public void onClick()
              {
                final PageParameters params = new PageParameters();
                params.add(SkillRatingListPage.PARAM_SKILL_ID, id);
                final SkillRatingListPage skillRatingListPage = new SkillRatingListPage(params);
                skillRatingListPage.setReturnToPage(SkillEditPage.this);
                setResponsePage(skillRatingListPage);
              }
            }, getString("plugins.skillmatrix.skill.menu.addRating"));
        addContentMenuEntry(menu);
      }
    }
  }

}
