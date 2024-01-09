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
import org.apache.wicket.Component;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * A factory, which creates edit components for Attr Values.
 *
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface AttrWicketComponentFactory
{
  ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc, final EntityWithAttributes entity);

  default void setAndOutputMarkupId(final Component component, final AttrGroup group, final AttrDescription desc)
  {
    component.setMarkupId(group.getName() + "-" + desc.getPropertyName()).setOutputMarkupId(true);
  }
}
