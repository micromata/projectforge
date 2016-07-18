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

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one check-box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class AjaxEditableLabelPanel<T> extends Panel
{
  public static final String WICKET_ID = "label";

  private AjaxEditableLabel<T> ajaxEditableLabel;

  public AjaxEditableLabelPanel(final String id, final AjaxEditableLabel<T> ajaxEditableLabel)
  {
    super(id);
    add(this.ajaxEditableLabel = ajaxEditableLabel);
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public AjaxEditableLabelPanel<T> setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(ajaxEditableLabel, tooltip);
    return this;
  }
}
