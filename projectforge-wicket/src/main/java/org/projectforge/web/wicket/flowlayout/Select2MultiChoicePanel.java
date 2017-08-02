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

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketRenderHeadUtils;
import org.projectforge.web.wicket.WicketUtils;
import org.wicketstuff.select2.Select2MultiChoice;

/**
 * Panel containing only one drop down choice box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@SuppressWarnings("serial")
public class Select2MultiChoicePanel<T> extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "select2MultiChoice";

  private final Select2MultiChoice<T> select2MultiChoice;

  /**
   * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.head.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    WicketRenderHeadUtils.renderSelect2JavaScriptIncludes(response);
  }

  /**
   * @param id
   * @param label          see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   */
  public Select2MultiChoicePanel(final String id, final Select2MultiChoice<T> select2MultiChoice)
  {
    super(id);
    this.select2MultiChoice = select2MultiChoice;
    add(select2MultiChoice);
    setRenderBodyOnly(true);
  }

  /**
   * @return the select2MultiChoice
   */
  public Select2MultiChoice<T> getSelect2MultiChoice()
  {
    return select2MultiChoice;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    select2MultiChoice.setOutputMarkupId(true);
    return select2MultiChoice.getMarkupId();
  }

  /**
   * Sets tool-tip for the label.
   *
   * @param tooltip
   * @return this for chaining.
   */
  public Select2MultiChoicePanel<T> setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(select2MultiChoice, tooltip);
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return select2MultiChoice;
  }
}
