package org.projectforge.web.common.timeattr;

import java.util.function.Consumer;

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

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

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
