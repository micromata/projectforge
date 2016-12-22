package org.projectforge.web.wicket;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;

public class TimedAttrColumn<PK extends Serializable, T extends TimeableAttrRow<PK>, U extends EntityWithTimeableAttr<PK, T> & EntityWithConfigurableAttr>
    extends AbstractColumn<U, String>
{
  private final GuiAttrSchemaService guiAttrSchemaService;

  private final String groupName;

  private final String descName;

  private final CellItemListener<U> cellItemListener;

  public TimedAttrColumn(final GuiAttrSchemaService guiAttrSchemaService, final String groupName,
      final String descName, final CellItemListener<U> cellItemListener)
  {
    super(new Model<>(), String.format("attr:%s:%s", groupName, descName));
    this.guiAttrSchemaService = guiAttrSchemaService;
    this.groupName = groupName;
    this.descName = descName;
    this.cellItemListener = cellItemListener;
  }

  @Override
  public void populateItem(final Item<ICellPopulator<U>> item, final String componentId, final IModel<U> model)
  {
    final U entity = model.getObject();

    // set column heading
    if (getDisplayModel().getObject() == null) {
      final AttrDescription attrDescription = guiAttrSchemaService.getAttrDescription(entity, groupName, descName);
      final String i18nkey = attrDescription.getI18nkey();
      final String translation = I18nHelper.getLocalizedMessage(i18nkey);
      getDisplayModel().setObject(translation);
    }

    final Optional<IModel<String>> stringAttribute = guiAttrSchemaService.getStringAttribute(entity, new Date(), groupName, descName);

    if (stringAttribute.isPresent()) {
      item.add(new Label(componentId, stringAttribute.get()));
    } else {
      // empty label
      item.add(new Label(componentId));
    }

    if (cellItemListener != null) {
      cellItemListener.populateItem(item, componentId, model);
    }
  }

}
