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

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

import com.vaynberg.wicket.select2.Select2Choice;

/**
 * Panel containing only one drop down choice box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call
 * setRenderBodyOnly(false).
 * 
 * @author Florian Blumenstein
 * 
 */
@SuppressWarnings("serial")
public class Select2SingleChoicePanel<T> extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "select2SingleChoice";

  private final Select2Choice<T> select2Choice;

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   */
  public Select2SingleChoicePanel(final String id, final Select2Choice<T> select2Choice)
  {
    super(id);
    this.select2Choice = select2Choice;
    add(select2Choice);
  }

  /**
   * @return the select2Choice
   */
  public Select2Choice<T> getSelect2Choice()
  {
    return select2Choice;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    select2Choice.setOutputMarkupId(true);
    return select2Choice.getMarkupId();
  }

  /**
   * Sets tool-tip for the label.
   * 
   * @param tooltip
   * @return this for chaining.
   */
  public Select2SingleChoicePanel<T> setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(select2Choice, tooltip);
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return select2Choice;
  }
}
