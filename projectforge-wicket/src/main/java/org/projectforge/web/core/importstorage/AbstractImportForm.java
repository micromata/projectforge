/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.core.importstorage;

import java.util.Map;
import java.util.Set;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.projectforge.framework.persistence.utils.ImportStorage;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;

public abstract class AbstractImportForm<F, P extends AbstractImportPage<?>, S extends AbstractImportStoragePanel<?>> extends AbstractStandardForm<F, P>
{
  private static final long serialVersionUID = -334887092842775629L;

  protected FileUploadField fileUploadField;

  protected S storagePanel;

  protected ImportFilter importFilter;

  public AbstractImportForm(final P parentPage)
  {
    super(parentPage);
    initUpload(Bytes.megabytes(10));
    importFilter = new ImportFilter();
  }

  /**
   * @return this for chaining.
   */
  @SuppressWarnings("serial")
  protected AbstractImportForm<F, P, S> addClearButton(final FieldsetPanel fs)
  {
    fs.add(new SingleButtonPanel(fs.newChildId(), new Button(SingleButtonPanel.WICKET_ID, new Model<String>("clearStorage"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.clear();
      }
    }, getString("common.import.clearStorage"), SingleButtonPanel.RESET)
    {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return storagePanel.isVisible();
      }
    });
    return this;
  }

  /**
   * @return this for chaining.
   */
  @SuppressWarnings("serial")
  protected AbstractImportForm<F, P, S> addImportFilterRadio(final GridBuilder gridBuilder)
  {
    final FieldsetPanel fs = new FieldsetPanel(gridBuilder.getPanel(), getString("filter"))
    {
      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return storagePanel.isVisible();
      }
    };
    final DivPanel radioGroupPanel = fs.addNewRadioBoxButtonDiv();
    final RadioGroupPanel<String> radioGroup = new RadioGroupPanel<String>(radioGroupPanel.newChildId(), "filterType",
        new PropertyModel<String>(importFilter, "listType"), new FormComponentUpdatingBehavior());
    radioGroupPanel.add(radioGroup);
    fs.setLabelFor(radioGroup.getRadioGroup());
    radioGroup.add(new Model<String>("all"), getString("filter.all"));
    radioGroup.add(new Model<String>("modified"), getString("modified"));
    radioGroup.add(new Model<String>("faulty"), getString("filter.faulty"));
    return this;
  }

  @Override
  public void onBeforeRender()
  {
    refresh();
    super.onBeforeRender();
  }

  protected ImportStorage<?> getStorage()
  {
    return storagePanel.storage;
  }

  protected void setStorage(final ImportStorage<?> storage)
  {
    storagePanel.storage = storage;
  }

  protected void setErrorProperties(final Map<String, Set<Object>> errorProperties)
  {
    storagePanel.errorProperties = errorProperties;
  }

  protected void refresh()
  {
    storagePanel.storage = getStorage();
    if (storagePanel.storage == null) {
      storagePanel.storage = (ImportStorage<?>) parentPage.getUserPrefEntry(parentPage.getStorageKey());
    }
    if (storagePanel.storage == null) {
      storagePanel.setVisible(false);
      return;
    }
    storagePanel.setVisible(true);
    storagePanel.refresh();
  }
}
