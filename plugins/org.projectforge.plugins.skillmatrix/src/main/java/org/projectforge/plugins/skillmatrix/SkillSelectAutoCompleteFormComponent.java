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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

import java.util.List;
import java.util.Locale;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
public abstract class SkillSelectAutoCompleteFormComponent extends PFAutoCompleteTextField<SkillDO>
{

  private static final long serialVersionUID = -3142796647323340935L;

  public static final String I18N_KEY_ERROR_SKILL_NOT_FOUND = "plugins.skillmatrix.error.skillNotFound";

  @SpringBean
  private SkillDao skillDao;

  private SkillDO skill;

  public SkillSelectAutoCompleteFormComponent(final String id)
  {
    this(id, null);
    setModel(new PropertyModel<>(this, "skill"));
  }

  /**
   * @param id
   * @param model
   */
  public SkillSelectAutoCompleteFormComponent(final String id, final IModel<SkillDO> model)
  {
    super(id, model);
    getSettings().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false).withWidth(400);

    // Prevents a submit with an empty autocomplete textfield by pressing enter
    add(AttributeModifier.append("onkeypress", "if ( event.which == 13 ) { return false; }"));

    add(AttributeModifier.append("class", "mm_delayBlur"));

    add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 5394951486514219126L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        // AjaxRequestTarget needs this.
      }
    });
  }

  @Override
  protected List<SkillDO> getChoices(final String input)
  {
    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setSearchFields("title");
    filter.setSearchString(input);
    final List<SkillDO> list = skillDao.getList(filter);
    return list;
  }

  @Override
  protected String formatLabel(final SkillDO skill)
  {
    if (skill == null) {
      return "";
    }
    return createPath(skill.getId());
  }

  @Override
  protected String formatValue(final SkillDO skill)
  {
    if (skill == null) {
      return "";
    }
    return "" + skill.getId();
  }

  /**
   * create path to root
   *
   * @return
   */
  private String createPath(final Integer skillId)
  {
    final StringBuilder builder = new StringBuilder();
    final List<SkillNode> nodeList = getSkillTree().getPathToRoot(skillId);
    if (CollectionUtils.isEmpty(nodeList)) {
      return getString("task.path.rootTask");
    }
    final String pipeSeparator = " | ";
    String separator = "";
    for (final SkillNode node : nodeList) {
      builder.append(separator);
      builder.append(node.getSkill().getTitle());
      separator = pipeSeparator;
    }
    return builder.toString();
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    // this panel should always start with an empty input field, therefore delete the current model
    skill = null;
  }

  protected void notifyChildren()
  {
    final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
    if (target != null) {
      onModelSelected(target, skill);
    }
  }

  /**
   * Hook method which is called when the model is changed with a valid durin an ajax call
   *
   * @param target
   * @param taskDo
   */
  protected abstract void onModelSelected(final AjaxRequestTarget target, SkillDO skill);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    return new IConverter()
    {
      private static final long serialVersionUID = 6824608901238845695L;

      @Override
      public Object convertToObject(final String value, final Locale locale)
      {
        if (StringUtils.isEmpty(value)) {
          getModel().setObject(null);
          return null;
        }
        try {
          final SkillDO skill = getSkillTree().getSkillById(Integer.valueOf(value));
          if (skill == null) {
            error(getString(I18N_KEY_ERROR_SKILL_NOT_FOUND));
          }
          getModel().setObject(skill);

          notifyChildren();

          return skill;
        } catch (final NumberFormatException e) {
          // just ignore the NumberFormatException, because this could happen during wrong inputs
          return null;
        }
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

  public SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

}
