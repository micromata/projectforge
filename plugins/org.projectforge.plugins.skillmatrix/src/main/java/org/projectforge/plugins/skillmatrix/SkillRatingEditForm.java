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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Billy Duong (b.duong@micromata.de)
 *
 */
public class SkillRatingEditForm extends AbstractEditForm<SkillRatingDO, SkillRatingEditPage>
{
  private static final long serialVersionUID = -4997909992117525036L;

  private static final Logger log = LoggerFactory.getLogger(SkillRatingEditForm.class);

  public static final String I18N_KEY_ERROR_RATEABLE_SKILL_WITH_NULL_RATING = "plugins.skillmatrix.error.rateableSkillWithNullRating";

  public static final String I18N_KEY_ERROR_UNRATEABLE_SKILL_WITH_RATING = "plugins.skillmatrix.error.unrateableSkillWithRating";

  public static final String I18N_KEY_ERROR_SKILL_NOT_FOUND = "plugins.skillmatrix.error.skillNotFound";

  public static final String PARAM_SKILL_ID = "skillId";

  @SpringBean
  private SkillDao skillDao;

  @SpringBean
  private SkillRatingDao skillRatingDao;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  UserRightService userRights;

  // For AjaxRequest in skill and skill rating
  private FieldsetPanel fs;

  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  private boolean isUserInFullAccessGroup;

  /**
   * @param parentPage
   * @param data
   */
  public SkillRatingEditForm(final SkillRatingEditPage parentPage, final SkillRatingDO data)
  {
    super(parentPage, data);
    data.setUser(ThreadLocalUserContext.getUser());
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();

    final Integer[] curUserGroupIds = userDao.getAssignedGroups(ThreadLocalUserContext.getUser())
        .toArray(new Integer[0]);
    SkillDO skill = null;
    if (!isNew()) {
      skill = getData().getSkill();
    } else if (NumberHelper.greaterZero(this.getParentPage().skillId)) {
      skill = skillDao.getById(this.getParentPage().skillId);
    }

    isUserInFullAccessGroup = false;
    if (skill != null) {
      final Integer[] fullAccessGroupIds = ((SkillRight) userRights
          .getRight(SkillmatrixPluginUserRightId.PLUGIN_SKILL_MATRIX_SKILL))
              .getFullAccessGroupIds(skill);
      loop: for (final Integer i : curUserGroupIds) {
        for (final Integer j : fullAccessGroupIds) {
          if (i == j) {
            isUserInFullAccessGroup = true;
            break loop;
          }
        }
      }
    }

    add(new IFormValidator()
    {

      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @SuppressWarnings("unchecked")
      @Override
      public void validate(final Form<?> form)
      {
        final SkillSelectPanel skillSelectPanel = (SkillSelectPanel) dependentFormComponents[0];
        final DropDownChoice<SkillRating> skillRatingDropDown = (DropDownChoice<SkillRating>) dependentFormComponents[1];
        if (skillSelectPanel.getConvertedInput().getRateable()
            && skillRatingDropDown.getConvertedInput() == null) {
          error(getString(I18N_KEY_ERROR_RATEABLE_SKILL_WITH_NULL_RATING));
        } else if (!skillSelectPanel.getConvertedInput().getRateable()
            && skillRatingDropDown.getConvertedInput() != null) {
          error(getString(I18N_KEY_ERROR_UNRATEABLE_SKILL_WITH_RATING));
        }
      }

    });

    gridBuilder.newGridPanel();
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "user");
      if (!isUserInFullAccessGroup) {
        data.setUser(ThreadLocalUserContext.getUser());
        final DivTextPanel username = new DivTextPanel(fs.newChildId(), data.getUser().getUsername());
        username.setStrong();
        fs.add(username);
      } else {
        final UserSelectPanel attendeeSelectPanel = new UserSelectPanel(fs.newChildId(),
            new PropertyModel<>(data, "user"),
            parentPage, "userId");
        attendeeSelectPanel.init();
        fs.add(attendeeSelectPanel.setFocus().setRequired(true));
      }
    }
    gridBuilder.newGridPanel();
    {
      // Skill
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "skill");
      final SkillSelectPanel skillSelectPanel = new SkillSelectPanel(fs, new PropertyModel<>(data, "skill"),
          parentPage,
          "skillId")
      {
        @Override
        protected void onModelSelected(final AjaxRequestTarget target, final SkillDO skill)
        {
          super.onModelSelected(target, skill);
          if (target != null) {
            target.add(SkillRatingEditForm.this.fs.getFieldset());
          }
        }
      };
      fs.add(skillSelectPanel);
      fs.getFieldset().setOutputMarkupId(true);
      skillSelectPanel.init();
      skillSelectPanel.setRequired(true);
      dependentFormComponents[0] = skillSelectPanel;
    }
    {
      // Skill rating
      fs = gridBuilder.newFieldset(SkillRatingDO.class, "skillRating");
      fs.getFieldset().setOutputMarkupId(true);
      final LabelValueChoiceRenderer<SkillRating> ratingChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          SkillRating.values());
      final DropDownChoicePanel<SkillRating> skillChoice = new DropDownChoicePanel<SkillRating>(fs.newChildId(),
          new PropertyModel<>(data, "skillRating"), ratingChoiceRenderer.getValues(), ratingChoiceRenderer)
      {
        @Override
        public boolean isVisible()
        {
          if (data == null || data.getSkill() == null || !data.getSkill().getRateable()) {
            // If a skill is selected that is unrateable, reset the rating of the previous (probably rateable) skill.
            data.setSkillRating(null);
            return false;
          } else {
            return true;
          }
        }
      };
      fs.add(skillChoice);
      dependentFormComponents[1] = skillChoice.getDropDownChoice();
    }
    gridBuilder.newGridPanel();
    {
      // Since year
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "sinceYear");
      fs.add(
          new MinMaxNumberField<>(fs.getTextFieldId(), new PropertyModel<>(data, "sinceYear"), 0, 9000));
    }
    {
      // Certificates
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "certificates");
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "certificates")));
    }
    {
      // Training courses
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "trainingCourses");
      fs.add(new MaxLengthTextField(fs.getTextFieldId(), new PropertyModel<>(data, "trainingCourses")));
    }
    {
      // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "description"))).setAutogrow();
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(SkillRatingDO.class, "comment");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment"))).setAutogrow();
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

}
