package org.projectforge.web.common.timeattr;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;

/**
 * Compares an attribute determined by a given group name and description name.
 * The group and description names must be given by the sortProperty in the following format: "attr:group name:description name"
 */
public class AttrComperator<PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithTimeableAttr<PK, T> & EntityWithConfigurableAttr>
    implements Comparator<U>
{
  private final GuiAttrSchemaService guiAttrSchemaService = (GuiAttrSchemaService) ApplicationContextProvider.getApplicationContext()
      .getBean("guiAttrSchemaService");

  private final String sortProperty;

  private final boolean ascending;

  public AttrComperator(final String sortProperty, final boolean ascending)
  {
    this.sortProperty = sortProperty;
    this.ascending = ascending;
  }

  @Override
  public int compare(U o1, U o2)
  {
    final String[] strings = sortProperty.split(":");
    final String groupName = strings[1];
    final String descName = strings[2];

    final Date now = new Date();
    final Optional<IModel<String>> stringValue1 = guiAttrSchemaService.getStringAttribute(o1, now, groupName, descName);
    final Optional<IModel<String>> stringValue2 = guiAttrSchemaService.getStringAttribute(o2, now, groupName, descName);

    if (stringValue1.isPresent() && stringValue2.isPresent()) {
      final String sv1 = stringValue1.get().getObject();
      final String sv2 = stringValue2.get().getObject();
      return ascending ? sv1.compareTo(sv2) : sv2.compareTo(sv1);
    }

    return 0;
  }
}
