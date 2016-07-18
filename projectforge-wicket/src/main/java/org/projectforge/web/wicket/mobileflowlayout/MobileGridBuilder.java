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

package org.projectforge.web.wicket.mobileflowlayout;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.web.mobile.CollapsiblePanel;
import org.projectforge.web.wicket.flowlayout.AbstractGridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MobileGridBuilder extends AbstractGridBuilder<MobileFieldsetPanel>
{
  private static final long serialVersionUID = 134863232462613937L;

  WebMarkupContainer current;

  public MobileGridBuilder(final RepeatingView parent)
  {
    this.parentRepeatingView = parent;
  }

  public MobileGridBuilder(final DivPanel parent)
  {
    this.parentDivPanel = parent;
  }

  private String newParentChildId()
  {
    if (this.parentRepeatingView != null) {
      return this.parentRepeatingView.newChildId();
    } else {
      return this.parentDivPanel.newChildId();
    }
  }

  public CollapsiblePanel newCollapsiblePanel(final String heading)
  {
    final CollapsiblePanel collapsiblePanel = new CollapsiblePanel(newParentChildId(), heading);
    getParent().add(collapsiblePanel);
    current = collapsiblePanel;
    return collapsiblePanel;
  }

  public LabelValueDataTablePanel newLabelValueDataTable()
  {
    Validate.notNull(current);
    if (current instanceof CollapsiblePanel) {
      return new LabelValueDataTablePanel((CollapsiblePanel) current);
    }
    throw new UnsupportedOperationException("Please add collapsiblePanel to the GridBuilder first.");
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractGridBuilder#newFieldset(org.projectforge.web.wicket.flowlayout.FieldProperties)
   */
  @Override
  public MobileFieldsetPanel newFieldset(final FieldProperties< ? > fieldProperties)
  {
    Validate.notNull(current);
    if (current instanceof CollapsiblePanel) {
      return new MobileFieldsetPanel((CollapsiblePanel) current, fieldProperties);
    }
    throw new UnsupportedOperationException("Please add collapsiblePanel to the GridBuilder first.");
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractGridBuilder#newFieldset(java.lang.String, java.lang.String)
   */
  @Override
  public MobileFieldsetPanel newFieldset(final String labelText, final String labelDescription)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.GridBuilderInterface#newFieldset(java.lang.String)
   */
  @Override
  public MobileFieldsetPanel newFieldset(final Class<?> clazz, final String property)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * @see org.projectforge.web.wicket.flowlayout.AbstractGridBuilder#newFieldset(java.lang.String)
   */
  @Override
  public MobileFieldsetPanel newFieldset(final String label)
  {
    throw new UnsupportedOperationException();
  }
}
