package org.projectforge.framework.persistence.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.projectforge.export.AttrColumnDescription;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.hibernate.history.delta.PropertyDelta;

public class ImportedElementWithAttrs<PK extends Serializable, T extends TimeableAttrRow<PK>, E extends EntityWithTimeableAttr<PK, T>>
    extends ImportedElement<E>
{
  private final List<AttrColumnDescription> attrDiffProperties;
  private final Date dateToSelectAttrRow;
  private final TimeableService timeableService;

  public ImportedElementWithAttrs(final int index, final Class<E> clazz, final String[] diffProperties, final List<AttrColumnDescription> attrDiffProperties,
      final Date dateToSelectAttrRow, final TimeableService timeableService)
  {
    super(index, clazz, diffProperties);
    this.attrDiffProperties = attrDiffProperties;
    this.dateToSelectAttrRow = dateToSelectAttrRow;
    this.timeableService = timeableService;
  }

  @Override
  protected Collection<? extends PropertyDelta> addAdditionalPropertyDeltas()
  {
    if (attrDiffProperties == null) {
      return Collections.emptyList();
    }

    return attrDiffProperties
        .stream()
        .map(this::createPropertyDelta)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private PropertyDelta createPropertyDelta(final AttrColumnDescription colDesc)
  {
    final String fieldName = colDesc.getGroupName() + "." + colDesc.getPropertyName();
    final T attrRow = timeableService.getAttrRowForSameMonth(value, colDesc.getGroupName(), dateToSelectAttrRow);
    final T oldAttrRow = timeableService.getAttrRowForSameMonth(oldValue, colDesc.getGroupName(), dateToSelectAttrRow);
    if (attrRow == null) {
      return null;
    }

    final Object newVal = attrRow.getAttribute(colDesc.getPropertyName());
    final Object oldVal = (oldAttrRow == null) ? null : oldAttrRow.getAttribute(colDesc.getPropertyName());
    if (newVal == null && oldVal == null) {
      return null;
    }

    final Class type = (newVal != null) ? newVal.getClass() : oldVal.getClass();
    return createPropertyDelta(fieldName, newVal, oldVal, type).orElse(null);
  }
}
