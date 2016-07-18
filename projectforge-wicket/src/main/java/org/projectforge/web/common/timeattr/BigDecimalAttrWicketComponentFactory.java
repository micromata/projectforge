/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import java.math.BigDecimal;

import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * @author Roger Kommer (r.kommer.extern@micromata.de)
 *
 */
public class BigDecimalAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  /**
   * @see org.projectforge.web.common.timeattr.AttrWicketComponentFactory#createComponents(org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel,
   *      org.projectforge.framework.persistence.attr.api.AttrDescription,
   *      org.projectforge.framework.persistence.attr.api.EntityWithAttributes)
   */
  @Override
  public ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc, final EntityWithAttributes entity)
  {
    final MinMaxNumberField<BigDecimal> textField = new MinMaxNumberField<>(
        InputPanel.WICKET_ID,
        new AttrModel<>(entity, desc.getPropertyName(), BigDecimal.class),
        new BigDecimal(desc.getMinIntValue()),
        new BigDecimal(desc.getMaxIntValue())
    );
    setAndOutputMarkupId(textField, group, desc);
    textField.setRequired(desc.isRequired());

    return new InputPanel(id, textField);
  }

}
