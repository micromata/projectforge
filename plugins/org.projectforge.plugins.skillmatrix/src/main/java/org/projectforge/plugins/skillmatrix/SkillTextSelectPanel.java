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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.user.service.UserPreferencesHelper;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

import java.util.List;
import java.util.Locale;

/**
 * This panel shows the actual user and buttons for select/unselect training.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SkillTextSelectPanel extends AbstractSelectPanel<SkillDO> implements ComponentWrapperPanel
{

  private static final long serialVersionUID = 5388613518793987520L;

  private static final String USER_PREF_KEY_RECENT_SKILLS = "SkillTextSelectPanel:recentSkills";

  private boolean defaultFormProcessing = false;

  @SpringBean
  private SkillDao skillDao;

  private RecentQueue<String> recentSkills;

  private final PFAutoCompleteTextField<SkillDO> skillTextField;

  // Only used for detecting changes:
  private SkillDO currentSkill;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public SkillTextSelectPanel(final String id, final IModel<SkillDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(id, model, caller, selectProperty);

    skillTextField = new PFAutoCompleteTextField<SkillDO>("skillField", getModel())
    {
      @Override
      protected List<SkillDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("title", "description");
        filter.setSearchString(input);
        final List<SkillDO> list = skillDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentSkills().getRecents();
      }

      @Override
      protected String formatLabel(final SkillDO skill)
      {
        if (skill == null) {
          return "";
        }
        return formatSkill(skill);
      }

      @Override
      protected String formatValue(final SkillDO skill)
      {
        if (skill == null) {
          return "";
        }
        return formatSkill(skill);
      }

      @Override
      protected String getTooltip()
      {
        final SkillDO skill = getModel().getObject();
        if (skill == null || skill.getTitle() == null) {
          return null;
        }
        return skill.getTitle();
      }

      @Override
      public void convertInput()
      {
        final SkillDO skill = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(skill);
        if (skill != null && (currentSkill == null || skill.getId() != currentSkill.getId())) {
          getRecentSkills().append(formatSkill(skill));
        }
        currentSkill = skill;
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            if (StringUtils.isEmpty(value)) {
              getModel().setObject(null);
              return null;
            }
            // ### FORMAT ###
            final SkillDO skill = skillDao.getSkill(value);
            if (skill == null) {
              skillTextField.error(getString("plugins.skillmatrix.skilltraining.panel.error.skillNotFound"));
            }
            getModel().setObject(skill);
            return skill;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final SkillDO skill = (SkillDO) value;
            return skill.getTitle();
          }

        };
      }
    };
    currentSkill = getModelObject();
    skillTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(true)
        .withWidth(400);
  }

  /**
   * @see org.apache.wicket.markup.html.form.FormComponent#setLabel(org.apache.wicket.model.IModel)
   */
  @Override
  public SkillTextSelectPanel setLabel(final IModel<String> labelModel)
  {
    skillTextField.setLabel(labelModel);
    super.setLabel(labelModel);
    return this;
  }

  @Override
  public SkillTextSelectPanel init()
  {
    super.init();
    add(skillTextField);
    return this;
  }

  public void markTextFieldModelAsChanged()
  {
    skillTextField.modelChanged();
    final SkillDO skill = getModelObject();
    if (skill != null) {
      getRecentSkills().append(formatSkill(skill));
    }
  }

  public SkillTextSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    skillTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return skillTextField;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentSkills()
  {
    if (this.recentSkills == null) {
      this.recentSkills = (RecentQueue<String>) UserPreferencesHelper.getEntry(USER_PREF_KEY_RECENT_SKILLS);
    }
    if (this.recentSkills == null) {
      this.recentSkills = new RecentQueue<>();
      UserPreferencesHelper.putEntry(USER_PREF_KEY_RECENT_SKILLS, this.recentSkills, true);
    }
    return this.recentSkills;
  }

  private String formatSkill(final SkillDO skill)
  {
    if (skill == null) {
      return "";
    }
    // PLEASE NOTE: If you change the format don't forget to change the format above (search ### FORMAT ###)
    return skill.getTitle();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    skillTextField.setOutputMarkupId(true);
    return skillTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return skillTextField;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSelectPanel#setFocus()
   */
  @Override
  public AbstractSelectPanel<SkillDO> setFocus()
  {
    WicketUtils.setFocus(this.skillTextField);
    return this;
  }

  /**
   * Should be called before init() method. If true, then the validation will be done after submitting.
   *
   * @param defaultFormProcessing
   */
  public void setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    this.defaultFormProcessing = defaultFormProcessing;
  }

  /**
   * @return the defaultFormProcessing
   */
  public boolean isDefaultFormProcessing()
  {
    return defaultFormProcessing;
  }

}
