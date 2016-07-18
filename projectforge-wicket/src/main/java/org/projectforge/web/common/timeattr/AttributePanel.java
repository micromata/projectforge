package org.projectforge.web.common.timeattr;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

public class AttributePanel extends BaseAttributePanel
{
  public AttributePanel(final String id, final AttrGroup attrGroup, final EntityWithAttributes entity)
  {
    super(id, attrGroup);
    container.add(createContent(entity));
  }
}
