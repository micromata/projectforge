/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2016, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import java.util.Date;

import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * A Date input field.
 * 
 * @author Florian Blumenstein
 *
 */
public class DateAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  @Override
  public ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc,
      final EntityWithAttributes entity)
  {
    final DatePanel dp = new DatePanel(
        id,
        new AttrModel<>(entity, desc.getPropertyName(), Date.class),
        DatePanelSettings.get().withTargetType(java.sql.Date.class)
    );
    dp.setRequired(desc.isRequired());
    setAndOutputMarkupId(dp.getFormComponent(), group, desc);
    return dp;
  }

}
