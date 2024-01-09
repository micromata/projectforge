/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.common.timeattr;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

/**
 * Creates an Edit field for Integer
 * 
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class IntegerAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  /**
   * @see org.projectforge.web.common.timeattr.AttrWicketComponentFactory#createComponents(org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel,
   *      org.projectforge.framework.persistence.attr.api.AttrDescription,
   *      org.projectforge.framework.persistence.attr.api.EntityWithAttributes)
   */
  @Override
  public ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc, final EntityWithAttributes entity)
  {
    final MinMaxNumberField<Integer> textField = new MinMaxNumberField<>(
        InputPanel.WICKET_ID,
        new AttrModel<>(entity, desc.getPropertyName(), Integer.class), desc.getMinIntValue(),
        desc.getMaxIntValue()
    );
    setAndOutputMarkupId(textField, group, desc);
    textField.setRequired(desc.isRequired());

    return new InputPanel(id, textField);
  }

}
