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

package org.projectforge.web.address;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.*;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.sms.SmsSenderConfig;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.components.ExternalLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ListPage(editPage = AddressEditPage.class)
public class AddressListPage extends AbstractListPage<AddressListForm, AddressDao, AddressDO>
        implements IListPageColumnsCreator<AddressDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressListPage.class);

  private static final long serialVersionUID = 5168079498385464639L;

  public static final String PARAM_ADDRESSBOOKS = "addressbooks";

  private static final String APPLE_SCRIPT_DIR = "misc/";

  private static final String APPLE_SCRIPT_FOR_ADDRESS_BOOK = "AddressBookRemoveNotesOfClassWork.scpt";

  protected static final String[] MY_BOOKMARKABLE_INITIAL_PROPERTIES = mergeStringArrays(
          BOOKMARKABLE_INITIAL_PROPERTIES, new String[]{
                  "f.listType|lt", "f.uptodate", "f.outdated", "f.leaved", "f.active", "f.nonActive", "f.uninteresting",
                  "f.personaIngrata",
                  "f.departed"});

  @SpringBean
  private AddressDao addressDao;

  @SpringBean(name = "addressExport")
  private AddressExport addressExport;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private SmsSenderConfig smsSenderConfig;

  Map<Integer, PersonalAddressDO> personalAddressMap;

  boolean messagingSupported;

  boolean phoneCallSupported;

  public AddressListPage(final PageParameters parameters) {
    super(parameters, "address");
  }

  @Override
  public void renderHead(final IHeaderResponse response) {
    super.renderHead(response);
    response.render(JavaScriptReferenceHeaderItem.forUrl("scripts/zoom.js"));
  }

  @Override
  protected void setup() {
    super.setup();
    this.recentSearchTermsUserPrefKey = "addressSearchTerms";
    messagingSupported = smsSenderConfig.isSmsConfigured() == true;
    phoneCallSupported = configurationService.isTelephoneSystemUrlConfigured() == true;
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<AddressDO, String>> createColumns(final WebPage returnToPage, final boolean sortable) {
    final List<IColumn<AddressDO, String>> columns = new ArrayList<IColumn<AddressDO, String>>();
    final CellItemListener<AddressDO> cellItemListener = new CellItemListener<AddressDO>() {
      @Override
      public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                               final IModel<AddressDO> rowModel) {
        final AddressDO address = rowModel.getObject();
        final PersonalAddressDO personalAddress = personalAddressMap.get(address.getId());
        appendCssClasses(item, address.getId(), address.isDeleted());
        if (address.isDeleted() == true) {
          // Do nothing further
        } else if (personalAddress != null && personalAddress.isFavoriteCard() == true) {
          appendCssClasses(item, RowCssClass.FAVORITE_ENTRY);
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("modified")),
            getSortable("lastUpdate", sortable), "lastUpdate", cellItemListener) {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                               final IModel<AddressDO> rowModel) {
        final AddressDO address = rowModel.getObject();
        final RepeatingView view = new RepeatingView(componentId);
        item.add(view);
        view.add(
                new ListSelectActionPanel(view.newChildId(), rowModel, AddressEditPage.class, address.getId(), returnToPage,
                        DateTimeFormatter.instance().getFormattedDate(address.getLastUpdate())));

        view.add(new IconLinkPanel(view.newChildId(), IconType.PRINT, new ResourceModel("printView"),
                new Link<Void>(IconLinkPanel.LINK_ID) {

                  @Override
                  public void onClick() {
                    final PageParameters params = new PageParameters();
                    params.add(AbstractEditPage.PARAMETER_KEY_ID, address.getId());
                    final AddressViewPage addressViewPage = new AddressViewPage(params);
                    setResponsePage(addressViewPage);
                  }
                }));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("name")),
            getSortable("name", sortable), "name",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("firstName")),
            getSortable("firstName", sortable), "firstName", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("organization")),
            getSortable("organization",
                    sortable),
            "organization", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("email")), null, "email",
            cellItemListener) {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                               final IModel<AddressDO> rowModel) {
        final AddressDO address = rowModel.getObject();
        final RepeatingView view = new RepeatingView(componentId);
        item.add(view);
        boolean first = true;
        if (StringUtils.isNotBlank(address.getEmail()) == true) {
          final ExternalLinkPanel mailToLinkPanel = new ExternalLinkPanel(view.newChildId(),
                  "mailto:" + address.getEmail(), address
                  .getEmail());
          mailToLinkPanel.getLink().add(AttributeModifier.replace("onclick", "javascript:suppressNextRowClick();"));
          view.add(mailToLinkPanel);
          first = false;
        }
        if (StringUtils.isNotBlank(address.getPrivateEmail()) == true) {
          if (first == true) {
            first = false;
          } else {
            view.add(new Label(view.newChildId(), "<br/>").setEscapeModelStrings(false));
          }
          view.add(new ExternalLinkPanel(view.newChildId(), "mailto:" + address.getPrivateEmail(),
                  address.getPrivateEmail()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(
            new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("address.phoneNumbers")), null, null,
                    cellItemListener) {
              /**
               * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
               *      java.lang.String, org.apache.wicket.model.IModel)
               */
              @Override
              public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                                       final IModel<AddressDO> rowModel) {
                final AddressDO address = rowModel.getObject();
                final PersonalAddressDO personalAddress = personalAddressMap.get(address.getId());
                final RepeatingView view = new RepeatingView(componentId);
                item.add(view);
                final Integer id = address.getId();
                boolean first = addPhoneNumber(view, id, PhoneType.BUSINESS, address.getBusinessPhone(), false, IconType.BUILDING, true);
                first = addPhoneNumber(view, id, PhoneType.MOBILE, address.getMobilePhone(),true, IconType.TABLET, first);
                first = addPhoneNumber(view, id, PhoneType.PRIVATE, address.getPrivatePhone(), false, IconType.HOME, first);
                first = addPhoneNumber(view, id, PhoneType.PRIVATE_MOBILE, address.getPrivateMobilePhone(), true, IconType.TABLET, first);
                cellItemListener.populateItem(item, componentId, rowModel);
                item.add(AttributeModifier.append("style", new Model<String>("white-space: nowrap;")));
              }
            });
    columns.add(new CellItemListenerPropertyColumn<AddressDO>(new Model<String>(getString("address.addressbooks")), null, "address.addressbooks",
            cellItemListener) {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressDO>> item, final String componentId,
                               final IModel<AddressDO> rowModel) {
        final AddressDO address = rowModel.getObject();
        final RepeatingView view = new RepeatingView(componentId);
        item.add(view);
        boolean first = true;
        if (address.getAddressbookList() != null && address.getAddressbookList().size() > 0) {
          for (AddressbookDO addressbook : address.getAddressbookList()) {
            if (first == true) {
              first = false;
            } else {
              view.add(new Label(view.newChildId(), "<br/>").setEscapeModelStrings(false));
            }
            view.add(new Label(view.newChildId(), addressbook.getTitle()));
          }
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    return columns;
  }

  @SuppressWarnings("serial")
  @Override
  protected void init() {
    personalAddressMap = personalAddressDao.getPersonalAddressByAddressId();
    final List<IColumn<AddressDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "name", SortOrder.ASCENDING);
    form.add(dataTable);

    if (messagingSupported == true) {
      final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(),
              new Link<Object>("link") {
                @Override
                public void onClick() {
                  setResponsePage(SendSmsPage.class, new PageParameters());
                }
              }, getString("address.tooltip.writeSMS"));
      addContentMenuEntry(menuEntry);
    }
    if (WebConfiguration.isDevelopmentMode() == true) {
      // final Import vcards
      final BookmarkablePageLink<AddressImportPage> importVCardsLink = new BookmarkablePageLink<AddressImportPage>(
              ContentMenuEntryPanel.LINK_ID, AddressImportPage.class);

      final ContentMenuEntryPanel importVCardsButton = new ContentMenuEntryPanel(getNewContentMenuChildId(),
              importVCardsLink,
              getString("address.book.vCardImport")).setTooltip(getString("address.book.vCardImport.tooltip"));
      addContentMenuEntry(importVCardsButton);
    }
    final ContentMenuEntryPanel exportMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(), getString("export"));
    addContentMenuEntry(exportMenu);
    {
      // Export vcards
      final SubmitLink exportVCardsLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
        @Override
        public void onSubmit() {
          log.info("Exporting personal address book.");
          final List<PersonalAddressDO> list = addressDao.getFavoriteVCards();
          if (CollectionUtils.isEmpty(list) == true) {
            form.addError("address.book.hasNoVCards");
            return;
          }
          final String filename = "ProjectForge-PersonalAddressBook_" + DateHelper.getDateAsFilenameSuffix(new Date())
                  + ".vcf";
          final StringWriter writer = new StringWriter();
          addressDao.exportFavoriteVCards(writer, list);
          DownloadUtils.setUTF8CharacterEncoding(getResponse());
          DownloadUtils.setDownloadTarget(writer.toString().getBytes(), filename);
        }
      };
      final ContentMenuEntryPanel exportVCardsButton = new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(),
              exportVCardsLink,
              getString("address.book.vCardExport")).setTooltip(getString("address.book.vCardExport.tooltip.title"),
              getString("address.book.vCardExport.tooltip.content"));
      exportMenu.addSubMenuEntry(exportVCardsButton);
    }
    {
      // Excel export
      final SubmitLink excelExportLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
        @Override
        public void onSubmit() {
          log.info("Exporting address list.");
          final List<AddressDO> list = getList();
          final byte[] xls = addressExport.export(list, personalAddressMap);
          if (xls == null || xls.length == 0) {
            form.addError("address.book.hasNoVCards");
            return;
          }
          final String filename = "ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(new Date())
                  + ".xls";
          DownloadUtils.setDownloadTarget(xls, filename);
        }
      };
      final ContentMenuEntryPanel excelExportButton = new ContentMenuEntryPanel(exportMenu.newSubMenuChildId(),
              excelExportLink,
              getString("address.book.export")).setTooltip(getString("address.book.export"),
              getString("address.book.export.tooltip"));
      exportMenu.addSubMenuEntry(excelExportButton);
    }
    {
      final ContentMenuEntryPanel extendedMenu = contentMenuBarPanel.ensureAndAddExtendetMenuEntry();
      // Apple script export
      final SubmitLink appleScriptLink = new SubmitLink(ContentMenuEntryPanel.LINK_ID, form) {
        @Override
        public void onSubmit() {
          byte[] content = null;
          final String file = APPLE_SCRIPT_DIR + APPLE_SCRIPT_FOR_ADDRESS_BOOK;
          try {
            final ClassLoader cLoader = this.getClass().getClassLoader();
            final InputStream is = cLoader.getResourceAsStream(file);
            if (is == null) {
              log.error("Could not find script in resource path: '" + file + "'.");
            }
            content = IOUtils.toByteArray(is);
          } catch (final IOException ex) {
            log.error("Could not load script '" + file + "'." + ex.getMessage(), ex);
            throw new RuntimeException(ex);
          }
          DownloadUtils.setDownloadTarget(content, APPLE_SCRIPT_FOR_ADDRESS_BOOK);
        }
      };
      final ContentMenuEntryPanel appleScriptButton = new ContentMenuEntryPanel(extendedMenu.newSubMenuChildId(),
              appleScriptLink,
              getString("address.book.export.appleScript4Notes")).setTooltip(
              getString("address.book.export.appleScript4Notes"),
              getString("address.book.export.appleScript4Notes.tooltip"));
      extendedMenu.addSubMenuEntry(appleScriptButton);
    }
  }

  private boolean addPhoneNumber(final RepeatingView view, final Integer addressId, final PhoneType phoneType,
                                 final String phoneNumber,
                                 final boolean sendSms, final IconType icon, final boolean first) {
    if (StringUtils.isBlank(phoneNumber) == true) {
      return first;
    }
    if (first == false) {
      view.add(new Label(view.newChildId(), "<br/>").setEscapeModelStrings(false).setRenderBodyOnly(true));
    }
    final AddressListPhoneNumberPanel phoneNumberPanel = new AddressListPhoneNumberPanel(view.newChildId(), this,
            addressId, phoneType,
            phoneNumber, sendSms, icon, first);
    view.add(phoneNumberPanel);
    return false;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBookmarkableInitialProperties()
   */
  @Override
  protected String[] getBookmarkableInitialProperties() {
    return MY_BOOKMARKABLE_INITIAL_PROPERTIES;
  }

  @Override
  public void refresh() {
    super.refresh();
    if (form.getSearchFilter().isNewest() == true
            && StringUtils.isBlank(form.getSearchFilter().getSearchString()) == true) {
      form.getSearchFilter().setMaxRows(form.getPageSize());
    }
  }

  @Override
  protected AddressListForm newListForm(final AbstractListPage<?, ?, ?> parentPage) {
    return new AddressListForm(this);
  }

  @Override
  public AddressDao getBaseDao() {
    return addressDao;
  }

  protected AddressDao getAddressDao() {
    return addressDao;
  }
}
