/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.ExcelSheet;
import de.micromata.merlin.excel.ExcelWorkbook;
import de.micromata.merlin.excel.importer.ImportStatus;
import de.micromata.merlin.excel.importer.ImportStorage;
import de.micromata.merlin.excel.importer.ImportedElement;
import de.micromata.merlin.excel.importer.ImportedSheet;
import de.micromata.merlin.importer.PropertyDelta;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.common.StringHelper;
import org.projectforge.web.dialog.ModalQuestionDialog;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.PlainLabel;
import org.projectforge.web.wicket.flowlayout.DiffTextPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractImportStoragePanel<P extends AbstractImportPage<?>> extends Panel {
    private static final long serialVersionUID = 6755444819211298966L;

    protected WebMarkupContainer errorPropertiesTable;

    protected RepeatingView sheetRepeatingView;

    private Label storageHeadingLabel;

    protected Map<String, Set<Object>> errorProperties;

    protected LoadableDetachableModel<ImportStorage> storageModel;

    protected P parentPage;

    protected ImportFilter filter;

    private final MyModalQuestionDialog commitDialog;

    /**
     * @param id
     */
    public AbstractImportStoragePanel(final String id, final P parentPage, final ImportFilter filter) {
        super(id);
        this.parentPage = parentPage;
        this.filter = filter;
        sheetRepeatingView = new RepeatingView("sheetRepeater");
        add(sheetRepeatingView);
        commitDialog = new MyModalQuestionDialog();
        parentPage.add(commitDialog);
        commitDialog.init();
        storageModel = new LoadableDetachableModel<>() {
            @Override
            protected ImportStorage load() {
                return (ImportStorage<?>) parentPage.getUserPrefEntry(parentPage.getStorageKey());
            }
        };
    }

    public ImportStorage getStorage() {
        return storageModel.getObject();
    }

    public void clearStorage() {
        storageModel.setObject(null);
        parentPage.removeUserPrefEntry(parentPage.getStorageKey());
    }


    protected void setStorage(ImportStorage<?> storage) {
        parentPage.putUserPrefEntry(parentPage.getStorageKey(), storage, false);
        storageModel.setObject(storage);
    }

    public void refresh() {
        if (errorPropertiesTable != null) {
            remove(errorPropertiesTable);
        }
        if (storageHeadingLabel != null) {
            remove(storageHeadingLabel);
        }
        ImportStorage<?> storage = getStorage();
        add(storageHeadingLabel = new Label("storageHeading",
                "Import storage: " + (storage != null ? storage.getFilename() : "")))
                .setRenderBodyOnly(true);

        add(errorPropertiesTable = new WebMarkupContainer("errorPropertiesTable"));
        if (MapUtils.isNotEmpty(errorProperties) == true) {
            final RepeatingView errorPropertiesView = new RepeatingView("errorProperties");
            errorPropertiesTable.add(errorPropertiesView);
            for (final Map.Entry<String, Set<Object>> entry : errorProperties.entrySet()) {
                final WebMarkupContainer entryContainer = new WebMarkupContainer(errorPropertiesView.newChildId());
                errorPropertiesView.add(entryContainer);
                entryContainer.add(new Label("propertyKey", entry.getKey()));
                final StringBuilder buf = new StringBuilder();
                boolean first = true;
                for (final Object value : entry.getValue()) {
                    first = StringHelper.append(buf, first, String.valueOf(value), ", ");
                }
                entryContainer.add(new Label("propertyItems", buf.toString()));
            }
        } else {
            errorPropertiesTable.setVisible(false);
        }
        if (errorPropertiesTable == null) {
            add(errorPropertiesTable = new WebMarkupContainer("errorPropertiesTable")).setVisible(false);
        }
        if (storageHeadingLabel == null) {
            add(storageHeadingLabel = (Label) new Label("storageHeading", "[invisible]").setVisible(false));
        }
        sheetRepeatingView.removeAll();
        if (storage.getSheets() != null) {
            for (final ImportedSheet<?> sheet : storage.getSheets()) {
                final String sheetName = sheet.getName();
                IModel<ImportedSheet<?>> sheetModel = new LoadableDetachableModel<>() {
                    @Override
                    protected ImportedSheet load() {
                        return getStorage().getNamedSheet(sheetName);
                    }
                };
                addSheet(sheetModel);
            }
        }
    }

    @SuppressWarnings("serial")
    protected void addSheet(final IModel<ImportedSheet<?>> sheetModel) {
        final WebMarkupContainer cont = new WebMarkupContainer(sheetRepeatingView.newChildId());
        sheetRepeatingView.add(cont);
        StringBuilder buf = new StringBuilder();
        ImportedSheet<?> outerSheet = sheetModel.getObject(); // Don't use outerSheet in inner classes due to Wicket serialization issues!
        final String sheetName = outerSheet.getName();
        buf.append("Sheet: ").append(outerSheet.getName()).append(" ");
        if (outerSheet.isReconciled() == true) {
            buf.append(getString(outerSheet.getStatus().getI18nKey())).append(" ");
            if (outerSheet.getNumberOfCommittedElements() >= 0) {
                buf.append(": #").append(outerSheet.getNumberOfCommittedElements());
            }
        } else {
            buf.append(getString(ImportStatus.NOT_RECONCILED.getI18nKey()));
        }
        cont.add(new Label("sheetName", buf.toString()));
        final SubmitLink toggleLink = new SubmitLink("toggle") {
            @Override
            public void onSubmit() {
                ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                sheet.setOpen(!sheet.isOpen()); // Toggle open status.
            }
        };
        cont.add(toggleLink);
        toggleLink.add(new IconPanel("zoomInImage", IconType.ZOOM_IN) {
            @Override
            public boolean isVisible() {
                ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                return !sheet.isOpen();
            }
        });
        toggleLink.add(new IconPanel("zoomOutImage", IconType.ZOOM_OUT) {
            @Override
            public boolean isVisible() {
                ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                return sheet.isOpen();
            }
        });
        buf = new StringBuilder();
        buf.append("Total=").append(outerSheet.getTotalNumberOfElements()).append(" ");
        if (outerSheet.getNumberOfNewElements() > 0) {
            buf.append(" | New=<span style=\"color: red;\">").append(outerSheet.getNumberOfNewElements()).append("</span>");
        }
        if (outerSheet.getNumberOfModifiedElements() > 0) {
            buf.append(" | Modified=<span style=\"color: red;\">").append(outerSheet.getNumberOfModifiedElements())
                    .append("</span>");
        }
        if (outerSheet.getNumberOfUnmodifiedElements() > 0) {
            buf.append(" | Unmodified=").append(outerSheet.getNumberOfUnmodifiedElements());
        }
        if (outerSheet.getNumberOfFaultyElements() > 0) {
            buf.append(" | Errors=<span style=\"color: red; font-weight: bold;\">").append(outerSheet.getNumberOfFaultyElements())
                    .append("</span>");
        }
        cont.add(new PlainLabel("statistics", buf.toString()).setEscapeModelStrings(false));
        final RepeatingView actionLinkRepeater = new RepeatingView("actionLinkRepeater");
        cont.add(actionLinkRepeater);
        if (outerSheet.isReconciled() == false
                || outerSheet.getStatus().isIn(ImportStatus.IMPORTED, ImportStatus.NOTHING_TODO, ImportStatus.HAS_ERRORS) == true) {
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                    parentPage.reconcile(sheet.getName());
                }
            }, getString("common.import.action.reconcile"), getString("common.import.action.reconcile.tooltip"));
        } else if (outerSheet.isReconciled() == true) {
            addActionLink(actionLinkRepeater, new AjaxSubmitLink("actionLink", parentPage.form) {
                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    commitDialog.sheetName = sheetName;
                    commitDialog.open(target);
                }
            }, getString("common.import.action.commit"), getString("common.import.action.commit.tooltip"));
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    parentPage.selectAll(sheetName);
                }
            }, getString("common.import.action.selectAll"));
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    parentPage.select(sheetName, 100);
                }
            }, getString("common.import.action.select100"));
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    parentPage.select(sheetName, 500);
                }
            }, getString("common.import.action.select500"));
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    parentPage.deselectAll(sheetName);
                }
            }, getString("common.import.action.deselectAll"));
        }
        if (outerSheet.isFaulty() == true) {
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    parentPage.showErrorSummary(sheetName);
                }
            }, getString("common.import.action.showErrorSummary"));
        }
        ImportStorage<?> storage = getStorage();
        if (outerSheet.getLogger().getHasErrorEvents() || storage.getLogger().getHasErrorEvents()) {
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                    parentPage.downloadErrorLog(sheet);
                }
            }, getString("common.import.action.showErrorLog"));
        }
        if (outerSheet.getLogger().getHasEvents() || storage.getLogger().getHasEvents() || outerSheet.getLogger().getExcelSheet() != null) {
            addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                @Override
                public void onSubmit() {
                    ImportedSheet<?> sheet = sheetModel.getObject(); // Don't use sheet in inner classes due to Wicket serialization issues!
                    parentPage.downloadInfoLog(sheet);
                }
            }, getString("common.import.action.showInfoLog"));
        }
        try (final ExcelWorkbook excelWorkbook = parentPage.getStorage().getWorkbook()) {
            final ExcelSheet excelSheet = excelWorkbook != null ? excelWorkbook.getSheet(outerSheet.getOrigName()) : null;
            if (excelSheet != null && excelSheet.hasValidationErrors()) {
                addActionLink(actionLinkRepeater, new SubmitLink("actionLink") {
                    @Override
                    public void onSubmit() {
                        excelWorkbook.setActiveSheet(excelSheet.getSheetIndex());
                        parentPage.downloadValidatedExcel(sheetName);
                    }
                }, getString("common.import.action.downloadValidatedExcel"));
            }
            appendSheetActionLinks(outerSheet.getName(), actionLinkRepeater);
            addSheetTable(outerSheet, cont);
        }
    }

    /**
     * Does nothing at default.
     *
     * @param actionLinkRepeater
     */
    protected void appendSheetActionLinks(final String sheetName, final RepeatingView actionLinkRepeater) {
    }

    protected void addActionLink(final RepeatingView actionLinkRepeater, final AbstractLink link, final String label) {
        addActionLink(actionLinkRepeater, link, label, null);
    }

    protected void addActionLink(final RepeatingView actionLinkRepeater, final AbstractLink link, final String labelText,
                                 final String tooltip) {
        final WebMarkupContainer actionLinkContainer = new WebMarkupContainer(actionLinkRepeater.newChildId());
        actionLinkRepeater.add(actionLinkContainer);
        final Label label = new Label("label", labelText);
        if (tooltip != null) {
            WicketUtils.addTooltip(label, tooltip);
        }
        actionLinkContainer.add(link.add(label));
    }

    protected abstract void addHeadColumns(final RepeatingView headColRepeater);

    protected abstract void addColumns(final RepeatingView cellRepeater, final ImportedElement<?> element,
                                       final String style);

    private void addSheetTable(final ImportedSheet<?> sheet, final WebMarkupContainer container) {
        final WebMarkupContainer table = new WebMarkupContainer("sheetTable");
        container.add(table);
        final List<?> elements = sheet.getElements();
        if (sheet.isOpen() == false || CollectionUtils.isEmpty(elements) == true) {
            table.setVisible(false);
            return;
        }
        final RepeatingView headColRepeater = new RepeatingView("headColRepeater");
        table.add(headColRepeater);
        addHeadColumns(headColRepeater);
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("modifications")));
        headColRepeater.add(new Label(headColRepeater.newChildId(), getString("errors")));
        final RepeatingView rowRepeater = new RepeatingView("rowRepeater");
        table.add(rowRepeater);
        int row = 0;
        for (final ImportedElement<?> element : sheet.getElements()) {
            final String listType = filter.getListType();
            if ("all".equals(listType) == true //
                    || ("faulty".equals(listType) == true && element.isFaulty() == true)//
                    || ("modified".equals(listType) == true
                    && (element.isNew() == true || element.isModified() == true || element.isFaulty() == true)) //
            ) {
                // Yes, show this element.
            } else {
                // Don't show this element.
                continue;
            }
            final WebMarkupContainer rowContainer = new WebMarkupContainer(rowRepeater.newChildId());
            rowRepeater.add(rowContainer);
            rowContainer.add(AttributeModifier.replace("class", (row++ % 2 == 0) ? "even" : "odd"));
            rowContainer.add(AttributeModifier.replace("onmousedown", "javascript:rowCheckboxClick(this, event);"));
            final String style;
            if (element.isFaulty() == true) {
                style = "color: red;";
            } else if (element.getOldValue() != null && element.getValue() == null) {
                style = "text-decoration: line-through;";
            } else {
                style = null;
            }
            final WebMarkupContainer firstCell = new WebMarkupContainer("firstCell");
            if (style != null) {
                firstCell.add(AttributeModifier.replace("style", style));
            }
            rowContainer.add(firstCell);
            final String sheetName = sheet.getName();
            final Integer elementRow = element.getRow();
            IModel<ImportedElement<?>> elementModel = new LoadableDetachableModel<>() {
                @Override
                protected ImportedElement<?> load() {
                    List<ImportedElement<?>> elements = storageModel.getObject().getNamedSheet(sheetName).getElements();
                    // Java version of elements.find {it.row == elementRow}:
                    return elements.stream()
                            .filter(it -> it.getRow() == elementRow)
                            .findFirst()
                            .orElse(null);
                }
            };
            IModel<Boolean> selectedModel = new LoadableDetachableModel<>() {
                @Override
                protected Boolean load() {
                    ImportedElement<?> element = elementModel.getObject();
                    return element != null && element.getSelected();
                }
                @Override
                public void setObject(Boolean selected) {
                    ImportedElement<?> element = elementModel.getObject();
                    if (element != null) {
                        element.setSelected(selected);
                    }
                }
            };
            final CheckBox checkBox = new CheckBox("selectItem", selectedModel);
            if (sheet.getStatus() != ImportStatus.RECONCILED) {
                checkBox.setVisible(false);
            }
            firstCell.add(checkBox);
            final IconType iconType;
            if (element.isNew() == true) {
                iconType = IconType.PLUS_SIGN;
            } else if (element.isModified() == true) {
                iconType = IconType.MODIFIED;
            } else {
                iconType = IconType.DOCUMENT;
            }
            firstCell.add(new IconPanel("icon", iconType));

            final RepeatingView cellRepeater = new RepeatingView("cellRepeater");
            rowContainer.add(cellRepeater);

            addColumns(cellRepeater, element, style);

            if (element.getOldValue() != null && element.getPropertyChanges() != null) {
                final StringBuilder buf = new StringBuilder();
                final StringBuilder oldValue = new StringBuilder();
                boolean first = true;
                // TODO HISTORY
                for (final PropertyDelta delta : element.getPropertyChanges()) {
                    StringHelper.append(buf, first, delta.getProperty(), "; ");
                    first = StringHelper.append(oldValue, first, delta.getProperty(), "; ");
                    buf.append("=").append(delta.getNewValue());
                    oldValue.append("=").append(delta.getOldValue());
                }
                final DiffTextPanel diffTextPanel = new DiffTextPanel("value", Model.of(buf.toString()),
                        Model.of(oldValue.toString()));
                addCell(cellRepeater, diffTextPanel, style);
            } else {
                addCell(cellRepeater, "", null);
            }
            if (element.isFaulty() == true) {
                final StringBuilder buf = new StringBuilder();
                if (element.getErrorProperties() != null) {
                    boolean first = true;
                    for (final Map.Entry<String, Object> entry : element.getErrorProperties().entrySet()) {
                        first = StringHelper.append(buf, first, entry.getKey(), ", ");
                        buf.append("=[").append(entry.getValue()).append("]");
                    }
                }
                addCell(cellRepeater, buf.toString(), " color: red; font-weight: bold;");
            } else {
                addCell(cellRepeater, "", null);
            }
        }
    }

    protected Component addCell(final RepeatingView cellRepeater, final Component comp, final String style) {
        final WebMarkupContainer cell = new WebMarkupContainer(cellRepeater.newChildId());
        cellRepeater.add(cell);
        cell.add(comp);
        if (style != null) {
            cell.add(AttributeModifier.replace("style", style));
        }
        return comp;
    }

    protected Component addCell(final RepeatingView cellRepeater, final String value, final String style) {
        final Component comp = new Label("value", StringUtils.defaultString(value));
        return addCell(cellRepeater, comp, style);
    }

    protected Component addCell(final RepeatingView cellRepeater, final Integer value, final String style) {
        if (value == null) {
            return addCell(cellRepeater, "", style);
        } else {
            return addCell(cellRepeater, String.valueOf(value), style);
        }
    }

    @SuppressWarnings("serial")
    private class MyModalQuestionDialog extends ModalQuestionDialog {
        String sheetName;

        MyModalQuestionDialog() {
            super(parentPage.newModalDialogId(), new ResourceModel("common.import.commitQuestionDialog.heading"),
                    new ResourceModel(
                            "common.import.commitQuestionDialog.question"));
        }

        /**
         * @see org.projectforge.web.dialog.ModalQuestionDialog#onCloseButtonSubmit(org.apache.wicket.ajax.AjaxRequestTarget)
         */
        @Override
        protected boolean onCloseButtonSubmit(final AjaxRequestTarget target) {
            super.onCloseButtonSubmit(target);
            if (isConfirmed() == true) {
                parentPage.commit(sheetName);
                PageParameters params = new PageParameters();
                params.set(0, "success");
                parentPage.setPageParametersOnSuccess(params);
                setResponsePage(parentPage.getClass(), params);
            }
            return true;
        }
    }
}
