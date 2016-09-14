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
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panel containing only one drop down choice box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ListMultipleChoicePanel<T> extends Panel implements ComponentWrapperPanel
{
  private static final long serialVersionUID = 4895728826834069512L;

  public static final String WICKET_ID = "multipleListChoice";

  private ListMultipleChoice<T> listChoice;

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   */
  public ListMultipleChoicePanel(final String id, final ListMultipleChoice<T> listChoice)
  {
    this(id, listChoice, false);
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param dropDownChoice
   * @param submitOnChange
   */
  public ListMultipleChoicePanel(final String id, final ListMultipleChoice<T> listChoice, final boolean submitOnChange)
  {
    super(id);
    this.listChoice = listChoice;
    add(listChoice);
    setRenderBodyOnly(true);
  }

  public ListMultipleChoicePanel<T> setRequired(final boolean required)
  {
    listChoice.setRequired(required);
    return this;
  }

  public ListMultipleChoice<T> getListMultipleChoice()
  {
    return listChoice;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    listChoice.setOutputMarkupId(true);
    return listChoice.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return listChoice;
  }
}
