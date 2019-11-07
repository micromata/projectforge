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

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
public class SkillRatingListForm extends AbstractListForm<SkillRatingFilter, SkillRatingListPage>
{
  private static final long serialVersionUID = 5333752125044497290L;

  private static final Logger log = LoggerFactory.getLogger(SkillRatingListForm.class);

  public static final String I18N_KEY_REQUIRED_EXPERIENCE = "plugins.skillmatrix.search.reqiuredExperience";

  @SpringBean
  private SkillDao skillDao;

  /**
   * @param parentPage
   */
  public SkillRatingListForm(final SkillRatingListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();
    {
      // Required experience
      gridBuilder.newSplitPanel(GridSize.COL100);
      FieldsetPanel fs = gridBuilder.newFieldset(getString(I18N_KEY_REQUIRED_EXPERIENCE)).suppressLabelForWarning();
      fs.getFieldset().setOutputMarkupId(true);
      final LabelValueChoiceRenderer<SkillRating> ratingChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          SkillRating.values());
      final DropDownChoicePanel<SkillRating> skillChoice = new DropDownChoicePanel<>(fs.newChildId(),
          new PropertyModel<>(getSearchFilter(), "skillRating"), ratingChoiceRenderer.getValues(),
          ratingChoiceRenderer);
      skillChoice.setNullValid(true);
      fs.add(skillChoice);

      // User
      gridBuilder.newSplitPanel(GridSize.COL50);
      fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skillrating.user"));
      @SuppressWarnings("serial")
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>()
      {
        @Override
        public PFUserDO getObject()
        {
          return getTenantRegistry().getUserGroupCache().getUser(getSearchFilter().getUserId());
        }

        @Override
        public void setObject(final PFUserDO object)
        {
          if (object == null) {
            getSearchFilter().setUserId(null);
          } else {
            getSearchFilter().setUserId(object.getId());
          }
        }
      }, parentPage, "userId");
      fs.add(userSelectPanel);
      userSelectPanel.setDefaultFormProcessing(false);
      userSelectPanel.init().withAutoSubmit(true);

      // Skill
      gridBuilder.newSplitPanel(GridSize.COL50);
      fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skillrating.skill"));
      @SuppressWarnings("serial")
      final SkillTextSelectPanel skillSelectPanel = new SkillTextSelectPanel(fs.newChildId(), new Model<SkillDO>()
      {
        @Override
        public SkillDO getObject()
        {
          return skillDao.getById(getSearchFilter().getSkillId());
        }

        @Override
        public void setObject(final SkillDO object)
        {
          if (object == null) {
            getSearchFilter().setSkillId(null);
          } else {
            getSearchFilter().setSkillId(object.getId());
          }
        }
      }, parentPage, "skillId");
      fs.add(skillSelectPanel);
      skillSelectPanel.setDefaultFormProcessing(false);
      skillSelectPanel.init().withAutoSubmit(true);

    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected SkillRatingFilter newSearchFilterInstance()
  {
    return new SkillRatingFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
