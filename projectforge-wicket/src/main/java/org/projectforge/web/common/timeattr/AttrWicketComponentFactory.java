/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import org.apache.wicket.Component;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

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
