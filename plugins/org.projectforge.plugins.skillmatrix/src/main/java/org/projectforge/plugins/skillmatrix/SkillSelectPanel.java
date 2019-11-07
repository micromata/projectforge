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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Hibernate;
import org.projectforge.web.CSSColor;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.util.List;
import java.util.ListIterator;

/**
 * @author Billy Duong (b.duong@micromata.de)
 */
public class SkillSelectPanel extends AbstractSelectPanel<SkillDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = -7231190025292695850L;

  public static final String I18N_KEY_SELECT_ANCESTOR_SKILL_TOOLTIP = "plugins.skillmatrix.skill.selectPanel.selectAncestorSkill.tooltip";

  public static final String I18N_KEY_DISPLAY_SKILL_TOOLTIP = "plugins.skillmatrix.skill.selectPanel.displaySkill.tooltip";

  public static final String I18N_KEY_SELECT_SKILL_TOOLTIP = "plugins.skillmatrix.skill.selectPanel.selectSkill";

  public static final String I18N_KEY_UNSELECT_SKILL_TOOLTIP = "plugins.skillmatrix.skill.selectPanel.unselectSkill";

  @SpringBean
  private SkillDao skillDao;

  private boolean showPath = true;

  private final WebMarkupContainer divContainer;

  private RepeatingView ancestorRepeater;

  private Integer currentSkillId;

  private boolean ajaxSkillSelectMode;

  private WebMarkupContainer userselectContainer;

  private FieldsetPanel fieldsetPanel;

  public SkillSelectPanel(final FieldsetPanel fieldsetPanel, final IModel<SkillDO> model,
      final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(fieldsetPanel.newChildId(), model, caller, selectProperty);
    this.fieldsetPanel = fieldsetPanel;
    fieldsetPanel.getFieldset().setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
    SkillDO skill = model.getObject();
    if (!Hibernate.isInitialized(skill)) {
      skill = getSkillTree().getSkillById(skill.getId());
      model.setObject(skill);
    }
    divContainer = new WebMarkupContainer("div")
    {
      private static final long serialVersionUID = -8150112323444983335L;

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        // display only, if we are not in ajax skill select mode
        return !ajaxSkillSelectMode;
      }
    };
    divContainer.setOutputMarkupId(true);
    divContainer.setOutputMarkupPlaceholderTag(true);
    add(divContainer);
    ajaxSkillSelectMode = false;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSelectPanel#onBeforeRender()
   */
  @SuppressWarnings("serial")
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    final SkillDO skill = getModelObject();
    final Integer skillId = skill != null ? skill.getId() : null;
    if (currentSkillId == skillId) {
      return;
    }
    currentSkillId = skillId;
    if (showPath && skill != null) {
      ancestorRepeater.removeAll();
      final SkillNode skillNode = getSkillTree().getSkillNodeById(skill.getId());
      final List<Integer> ancestorIds = skillNode.getAncestorIds();
      final ListIterator<Integer> it = ancestorIds.listIterator(ancestorIds.size());
      while (it.hasPrevious()) {
        final Integer ancestorId = it.previous();
        final SkillDO ancestorSkill = getSkillTree().getSkillById(ancestorId);
        if (ancestorSkill.getParent() == null) {
          // Don't show root node:
          continue;
        }
        final WebMarkupContainer cont = new WebMarkupContainer(ancestorRepeater.newChildId());
        ancestorRepeater.add(cont);
        final SubmitLink selectSkillLink = new SubmitLink("ancestorSkillLink")
        {
          @Override
          public void onSubmit()
          {
            caller.select(selectProperty, ancestorSkill.getId());
          }
        };
        selectSkillLink.setDefaultFormProcessing(false);
        cont.add(selectSkillLink);
        WicketUtils.addTooltip(selectSkillLink, getString(I18N_KEY_SELECT_ANCESTOR_SKILL_TOOLTIP));
        selectSkillLink.add(new Label("name", ancestorSkill.getTitle()));
      }
      ancestorRepeater.setVisible(true);
    } else {
      ancestorRepeater.setVisible(false);
    }
  }

  @Override
  @SuppressWarnings("serial")
  public SkillSelectPanel init()
  {
    super.init();
    ancestorRepeater = new RepeatingView("ancestorSkills");
    divContainer.add(ancestorRepeater);
    final SubmitLink skillLink = new SubmitLink("skillLink")
    {
      @Override
      public void onSubmit()
      {
        final SkillDO skill = getModelObject();
        if (skill == null) {
          return;
        }
        final PageParameters pageParams = new PageParameters();
        pageParams.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(skill.getId()));
        final SkillEditPage editPage = new SkillEditPage(pageParams);
        editPage.setReturnToPage((AbstractSecuredPage) getPage());
        setResponsePage(editPage);
      }
    };
    skillLink.setDefaultFormProcessing(false);
    divContainer.add(skillLink);
    // auto complete panels
    initAutoCompletePanels();

    WicketUtils.addTooltip(skillLink, getString(I18N_KEY_DISPLAY_SKILL_TOOLTIP));
    skillLink.add(new Label("name", new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        final SkillDO skill = getModelObject();
        return skill != null ? skill.getTitle() : "";
      }
    }));

    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        final SkillTreePage skillTreePage = new SkillTreePage(caller, selectProperty);
        if (getModelObject() != null) {
          skillTreePage.setHighlightedRowId(getModelObject().getId()); // Preselect node for highlighting.
        }
        setResponsePage(skillTreePage);
      }
    };
    selectButton.setDefaultFormProcessing(false);
    divContainer.add(selectButton);
    selectButton.add(new IconPanel("selectHelp", IconType.TASK, getString(I18N_KEY_SELECT_SKILL_TOOLTIP)));
    final SubmitLink unselectButton = new SubmitLink("unselect")
    {
      @Override
      public void onSubmit()
      {
        caller.unselect(selectProperty);
      }

      @Override
      public boolean isVisible()
      {
        return !isRequired() && getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(false);
    divContainer.add(unselectButton);
    unselectButton.add(new IconPanel("unselectHelp", IconType.REMOVE_SIGN, getString(I18N_KEY_UNSELECT_SKILL_TOOLTIP))
        .setColor(CSSColor.RED));

    return this;
  }

  /**
   *
   */
  private void initAutoCompletePanels()
  {
    userselectContainer = new WebMarkupContainer("userselectContainer")
    {
      private static final long serialVersionUID = -4871020567729661148L;

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        // only show if we are in ajax select skill mode
        return ajaxSkillSelectMode;
      }
    };
    add(userselectContainer);
    userselectContainer.setOutputMarkupId(true);
    userselectContainer.setOutputMarkupPlaceholderTag(true);
    final SkillSelectAutoCompleteFormComponent searchSkillInput = new SkillSelectAutoCompleteFormComponent(
        "searchSkillInput")
    {
      private static final long serialVersionUID = -7741009167252308262L;

      /**
       * @see org.projectforge.web.skill.SkillSelectAutoCompleteFormComponent#onModelChanged(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onModelSelected(final AjaxRequestTarget target, final SkillDO skillDo)
      {
        ajaxSkillSelectMode = false;
        target.appendJavaScript("hideAllTooltips();");
        SkillSelectPanel.this.setModelObject(skillDo);
        SkillSelectPanel.this.onModelSelected(target, skillDo);
      }

    };
    userselectContainer.add(searchSkillInput);
    // opener link
    final WebMarkupContainer searchSkillInputOpen = new WebMarkupContainer("searchSkillInputOpen");
    WicketUtils.addTooltip(searchSkillInputOpen, getString("quickselect"));
    searchSkillInputOpen.add(new AjaxEventBehavior("click")
    {
      private static final long serialVersionUID = -938527474172868488L;

      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        ajaxSkillSelectMode = true;
        target.appendJavaScript("hideAllTooltips();");
        target.add(divContainer);
        target.add(userselectContainer);
        target.focusComponent(searchSkillInput);
      }
    });
    // close link
    final WebMarkupContainer searchSkillInputClose = new WebMarkupContainer("searchSkillInputClose");
    divContainer.add(searchSkillInputClose);
    searchSkillInputClose.add(new AjaxEventBehavior("click")
    {
      private static final long serialVersionUID = -4334830387094758960L;

      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        ajaxSkillSelectMode = false;
        target.appendJavaScript("hideAllTooltips();");
        target.add(divContainer);
        target.add(userselectContainer);
      }
    });
    userselectContainer.add(searchSkillInputClose);
    divContainer.add(searchSkillInputOpen);
  }

  /**
   * Hook method which is called, when the skill is set by auto complete field
   *
   * @param target
   * @param skillDo
   */
  protected void onModelSelected(final AjaxRequestTarget target, final SkillDO skillDo)
  {
    target.add(fieldsetPanel.getFieldset());
    target.add(divContainer);
    target.add(userselectContainer);
  }

  /**
   * Will be called if the user has chosen an entry of the skill favorites drop down choice.
   *
   * @param skill
   */
  protected void selectSkill(final SkillDO skill)
  {
    setModelObject(skill);
    caller.select(selectProperty, skill.getId());
  }

  @Override
  public Component getClassModifierComponent()
  {
    return divContainer;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  /**
   * If true (default) then the path from the root skill to the currently selected will be shown, otherwise only the
   * name of the skill is displayed.
   *
   * @param showPath
   */
  public void setShowPath(final boolean showPath)
  {
    this.showPath = showPath;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    divContainer.setOutputMarkupId(true);
    return divContainer.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return null;
  }

  public SkillTree getSkillTree()
  {
    return skillDao.getSkillTree();
  }

  /**
   * @return the currentSkillId
   */
  public Integer getCurrentSkillId()
  {
    return currentSkillId;
  }
}
