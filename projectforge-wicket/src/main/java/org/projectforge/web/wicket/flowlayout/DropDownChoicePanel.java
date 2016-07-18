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

package org.projectforge.web.wicket.flowlayout;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one drop down choice box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class DropDownChoicePanel<T> extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "dropDownChoice";

  private DropDownChoice<T> dropDownChoice;

  /**
   * 
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param model
   * @param values
   * @param renderer
   */
  public DropDownChoicePanel(final String id, final IModel<T> model, final List< ? extends T> values, final IChoiceRenderer<T> renderer)
  {
    this(id, new DropDownChoice<T>(WICKET_ID, model, values, renderer), false);
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param model
   * @param values
   * @param renderer
   * @param submitOnChange
   */
  public DropDownChoicePanel(final String id, final IModel<T> model, final IModel<List<T>> values, final IChoiceRenderer<T> renderer,
      final boolean submitOnChange)
  {
    this(id, new DropDownChoice<T>(WICKET_ID, model, values, renderer), submitOnChange);
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param model
   * @param values
   * @param renderer
   * @param submitOnChange
   */
  public DropDownChoicePanel(final String id, final IModel<T> model, final List< ? extends T> values, final IChoiceRenderer<T> renderer,
      final boolean submitOnChange)
  {
    this(id, new DropDownChoice<T>(WICKET_ID, model, values, renderer), submitOnChange);
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   */
  public DropDownChoicePanel(final String id, final DropDownChoice<T> dropDownChoice)
  {
    this(id, dropDownChoice, false);
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   * @param submitOnChange
   */
  public DropDownChoicePanel(final String id, final DropDownChoice<T> dropDownChoice, final boolean submitOnChange)
  {
    super(id);
    this.dropDownChoice = dropDownChoice;
    add(dropDownChoice);
    if (submitOnChange == true) {
      dropDownChoice.add(AttributeModifier.replace("onchange", "javascript:submit();"));
    }
    setRenderBodyOnly(true);
  }

  public DropDownChoicePanel<T> setNullValid(final boolean nullValid)
  {
    dropDownChoice.setNullValid(nullValid);
    return this;
  }

  public DropDownChoicePanel<T> setRequired(final boolean required)
  {
    dropDownChoice.setRequired(required);
    return this;
  }

  public DropDownChoice<T> getDropDownChoice()
  {
    return dropDownChoice;
  }

  /**
   * Adds attribute onchange="javascript:submit();"
   * @return This for chaining.
   */
  public DropDownChoicePanel<T> setAutoSubmit()
  {
    dropDownChoice.add(AttributeModifier.replace("onchange", "javascript:submit();"));
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    dropDownChoice.setOutputMarkupId(true);
    return dropDownChoice.getMarkupId();
  }

  /**
   * @param dropDownChoice
   */
  public void replaceWithDropDownChoice(final DropDownChoice<T> dropDownChoice)
  {
    if (this.dropDownChoice != null) {
      remove(this.dropDownChoice);
    }
    this.dropDownChoice = dropDownChoice;
    add(dropDownChoice);
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public DropDownChoicePanel<T> setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(dropDownChoice, tooltip);
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return dropDownChoice;
  }
}
