package org.projectforge.web.common.timeattr;

import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

public class BooleanAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  @Override
  public ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc, final EntityWithAttributes entity)
  {
    final CheckBoxPanel checkBox = new CheckBoxPanel(
        id,
        new AttrModel<>(entity, desc.getPropertyName(), Boolean.class),
        null
    );
    setAndOutputMarkupId(checkBox.getFormComponent(), group, desc);
    return checkBox;
  }

}
