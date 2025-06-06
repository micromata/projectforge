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

package org.projectforge.plugins.marketing;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.address.*;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.plugins.marketing.rest.AddressCampaignValuePagesRest;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.io.Serializable;
import java.util.*;

/**
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@ListPage(editPage = AddressCampaignValueEditPage.class)
public class AddressCampaignValueListPage extends AbstractListPage<AddressCampaignValueListForm, AddressDao, AddressDO>
        implements
        IListPageColumnsCreator<AddressDO> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(AddressCampaignValueListPage.class);

    private static final long serialVersionUID = -2418497742599443358L;

    Map<Long, PersonalAddressDO> personalAddressMap;

    Map<Long, AddressCampaignValueDO> addressCampaignValueMap;

    public AddressCampaignValueListPage(final PageParameters parameters) {
        super(parameters, "plugins.marketing.addressCampaignValue");
        newItemMenuEntry.setVisibilityAllowed(false);
    }

    @Override
    public List<IColumn<AddressDO, String>> createColumns(final WebPage returnToPage, final boolean sortable) {
        return createColumns(returnToPage, sortable, form.getSearchFilter(), personalAddressMap,
                addressCampaignValueMap);
    }

    @SuppressWarnings("serial")
    protected static final List<IColumn<AddressDO, String>> createColumns(final WebPage page, final boolean sortable,
                                                                          final AddressCampaignValueFilter searchFilter,
                                                                          final Map<Long, PersonalAddressDO> personalAddressMap,
                                                                          final Map<Long, AddressCampaignValueDO> addressCampaignValueMap) {

        final List<IColumn<AddressDO, String>> columns = new ArrayList<>();
        final CellItemListener<AddressDO> cellItemListener = new CellItemListener<AddressDO>() {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final Serializable highlightedRowId;
                if (page instanceof AbstractListPage<?, ?, ?>) {
                    highlightedRowId = ((AbstractListPage<?, ?, ?>) page).getHighlightedRowId();
                } else {
                    highlightedRowId = null;
                }
                final PersonalAddressDO personalAddress = personalAddressMap.get(address.getId());
                appendCssClasses(item, address.getId(), highlightedRowId, address.getDeleted());
                if (address.getDeleted()) {
                    // Do nothing further
                } else if (personalAddress != null && personalAddress.isFavoriteCard()) {
                    appendCssClasses(item, RowCssClass.FAVORITE_ENTRY);
                }
                if (address.getAddressStatus().isIn(AddressStatus.LEAVED, AddressStatus.OUTDATED)
                        || address.getContactStatus().isIn(ContactStatus.DEPARTED, ContactStatus.NON_ACTIVE,
                        ContactStatus.PERSONA_INGRATA,
                        ContactStatus.UNINTERESTING, ContactStatus.DEPARTED)) {
                    appendCssClasses(item, RowCssClass.MARKED_AS_DELETED);
                }
            }
        };
        columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<>(page.getString("created")),
                getSortable("created",
                        sortable),
                "created", cellItemListener) {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final AddressCampaignValueDO addressCampaignValue = addressCampaignValueMap.get(address.getId());
                final Long addressCampaignValueId = addressCampaignValue != null ? addressCampaignValue.getId() : null;
                item.add(new ListSelectActionPanel(componentId, rowModel, AddressCampaignValueEditPage.class,
                        addressCampaignValueId, page,
                        DateTimeFormatter.instance().getFormattedDateTime(address.getCreated()),
                        AddressCampaignValueEditPage.PARAMETER_ADDRESS_ID,
                        String.valueOf(address.getId()), AddressCampaignValueEditPage.PARAMETER_ADDRESS_CAMPAIGN_ID,
                        String.valueOf(searchFilter
                                .getAddressCampaignId())));
                addRowClick(item);
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<>(new Model<>(page.getString("name")),
                getSortable("name", sortable),
                "name", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<>(new Model<>(page.getString("firstName")),
                getSortable("firstName",
                        sortable),
                "firstName", cellItemListener));
        columns.add(
                new CellItemListenerPropertyColumn<>(new Model<>(page.getString("organization")), getSortable(
                        "organization", sortable), "organization", cellItemListener));
        columns.add(new CellItemListenerPropertyColumn<>(
                new Model<>(page.getString("address.contactStatus")), getSortable(
                "contactStatus", sortable),
                "contactStatus", cellItemListener));
        columns.add(new AbstractColumn<AddressDO, String>(new Model<>(page.getString("address.addressText"))) {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final String addressText = StringHelper.listToString("|", address.getMailingAddressText(),
                        address.getMailingZipCode()
                                + " "
                                + address.getMailingCity(),
                        address.getMailingCountry());
                String returnToCaller = "/wa/addressCampaignValuesList";
                final AddressEditLinkPanel addressEditLinkPanel = new AddressEditLinkPanel(componentId, returnToCaller, address,
                        addressText);
                item.add(addressEditLinkPanel);
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        columns.add(new AbstractColumn<AddressDO, String>(new Model<>(page.getString("email"))) {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final String addressText = StringHelper.listToString(" | ",
                        address.getEmail(),
                        address.getPrivateEmail());
                item.add(new Label(componentId, addressText));
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        columns.add(new CellItemListenerPropertyColumn<>(
                new Model<>(page.getString("address.addressStatus")), getSortable(
                "addressStatus", sortable),
                "addressStatus", cellItemListener));
        columns.add(new AbstractColumn<AddressDO, String>(new Model<>(page.getString("value"))) {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final Long id = address.getId();
                final AddressCampaignValueDO addressCampaignValue = addressCampaignValueMap.get(id);
                if (addressCampaignValue != null) {
                    item.add(new Label(componentId, addressCampaignValue.getValue()));
                    item.add(AttributeModifier.append("style", new Model<>("white-space: nowrap;")));
                } else {
                    item.add(new Label(componentId, ""));
                }
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        columns.add(new AbstractColumn<AddressDO, String>(new Model<>(page.getString("comment"))) {
            @Override
            public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                     final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final Long id = address.getId();
                final AddressCampaignValueDO addressCampaignValue = addressCampaignValueMap.get(id);
                if (addressCampaignValue != null) {
                    item.add(new Label(componentId, addressCampaignValue.getComment()));
                    item.add(AttributeModifier.append("style", new Model<>("white-space: nowrap;")));
                } else {
                    item.add(new Label(componentId, ""));
                }
                cellItemListener.populateItem(item, componentId, rowModel);
            }
        });
        return columns;
    }

    @Override
    protected void onBeforeRender() {
        WicketSupport.get(AddressCampaignValueDao.class).getAddressCampaignValuesByAddressId(addressCampaignValueMap, form.getSearchFilter());
        super.onBeforeRender();
    }

    @SuppressWarnings("serial")
    @Override
    protected void init() {
        personalAddressMap = WicketSupport.get(PersonalAddressDao.class).getPersonalAddressByAddressId();
        addressCampaignValueMap = new HashMap<>();
        dataTable = createDataTable(createColumns(this, true), "name", SortOrder.ASCENDING);
        form.add(dataTable);
        {
            // Excel export
            final SubmitLink excelExportLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
                @Override
                public void onSubmit() {
                    log.info("Exporting address list.");
                    setRefreshResultList();
                    final List<AddressDO> list = getList();
                    final byte[] xls = WicketSupport.get(AddressCampaignValueExport.class).export(list, personalAddressMap, addressCampaignValueMap,
                            form.getSearchFilter()
                                    .getAddressCampaign() != null ? form.getSearchFilter().getAddressCampaign().getTitle() : "");
                    if (xls == null || xls.length == 0) {
                        form.addError("address.book.hasNoVCards");
                        return;
                    }
                    final String filename = "ProjectForge-AddressCampaignValueExport_"
                            + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xlsx";
                    DownloadUtils.setDownloadTarget(xls, filename);
                }
            };
            final ContentMenuEntryPanel excelExportButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
                    excelExportLink,
                    getString("address.book.export")).setTooltip(getString("address.book.export.tooltip"));
            addContentMenuEntry(excelExportButton);
        }
        addNewMassSelect(AddressCampaignValuePagesRest.class, form.getSearchFilter().getAddressCampaignId());
    }

    /**
     * @see org.projectforge.web.wicket.AbstractListPage#buildList()
     */
    @Override
    protected List<AddressDO> buildList() {
        List<AddressDO> list = super.buildList();
        final String value = form.getSearchFilter().getAddressCampaignValue();
        if (!StringUtils.isEmpty(value)) {
            final List<AddressDO> origList = list;
            list = new ArrayList<>();
            for (final AddressDO address : origList) {
                final AddressCampaignValueDO addressCampaignValue = addressCampaignValueMap.get(address.getId());
                if (addressCampaignValue != null && addressCampaignValue.getValue() != null) {
                    if (value.equals(addressCampaignValue.getValue())) {
                        list.add(address);
                    }
                } else {
                    // address campaign value of the given address is not set:
                    if (AddressCampaignValueListForm.ADDRESS_CAMPAIGN_VALUE_UNDEFINED.equals(value)) {
                        // Filter all address campaign values without defined value:
                        list.add(address);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (form.getSearchFilter().isNewest()
                && StringUtils.isBlank(form.getSearchFilter().getSearchString())) {
            form.getSearchFilter().setMaxRows(form.getPageSize());
        }
    }

    @Override
    protected AddressCampaignValueListForm newListForm(final AbstractListPage<?, ?, ?> parentPage) {
        return new AddressCampaignValueListForm(this);
    }

    @Override
    public AddressDao getBaseDao() {
        return WicketSupport.get(AddressDao.class);
    }
}
