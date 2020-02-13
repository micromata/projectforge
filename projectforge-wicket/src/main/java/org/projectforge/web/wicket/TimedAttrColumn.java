/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.wicket;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithConfigurableAttr;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

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
