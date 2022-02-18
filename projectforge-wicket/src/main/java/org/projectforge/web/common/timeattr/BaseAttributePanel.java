/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.util.function.Consumer;

public abstract class BaseAttributePanel extends Panel
{
  @SpringBean
  protected GuiAttrSchemaService attrSchemaService;

  /**
   * The attr group this panel represents.
   */
  protected final AttrGroup attrGroup;

  protected final WebMarkupContainer container = new WebMarkupContainer("refreshingContainer");

  public BaseAttributePanel(final String id, final AttrGroup attrGroup)
  {
    super(id);
    this.attrGroup = attrGroup;

    add(container);
    container.setOutputMarkupId(true);

    final String i18nKey = attrGroup.getI18nKey();
    final String heading = (i18nKey != null) ? getString(i18nKey) : null;
    container.add(new Label("heading", heading));
  }

  protected DivPanel createContent(final EntityWithAttributes entity)
  {
    return createContent(entity, null);
  }

  protected DivPanel createContent(final EntityWithAttributes entity, final Consumer<GridBuilder> gridBuilderConsumer)
  {
    final GridBuilder gridBuilder = new GridBuilder(null, "content");
    gridBuilder.newSplitPanel(GridSize.COL100);

    // this can be used to create content before the rest of the content is created
    if (gridBuilderConsumer != null) {
      gridBuilderConsumer.accept(gridBuilder);
    }

    // create the content according to the descriptions of this group (from the xml file)
    for (final AttrDescription desc : attrGroup.getDescriptions()) {
      final String label = getString(desc.getI18nkey());
      final FieldsetPanel fs = gridBuilder.newFieldset(label);
      final ComponentWrapperPanel component = attrSchemaService.createWicketComponent(fs.newChildId(), attrGroup, desc, entity);
      fs.add((Component) component);
    }

    return gridBuilder.getMainContainer();
  }
}
