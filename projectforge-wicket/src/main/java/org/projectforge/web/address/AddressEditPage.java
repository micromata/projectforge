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

package org.projectforge.web.address;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookRight;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.image.ImageService;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;

@EditPage(defaultReturnPage = AddressListPage.class)
public class AddressEditPage extends AbstractEditPage<AddressDO, AddressEditForm, AddressDao>
{
  private static final long serialVersionUID = 7091721062661400435L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressEditPage.class);

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private ImageService imageService;

  @SpringBean
  private UserRightService userRights;

  private boolean cloneFlag = false;

  private AddressDO clonedAddress;

  @SuppressWarnings("serial")
  public AddressEditPage(final PageParameters parameters)
  {
    super(parameters, "address");
    init();
    if (isNew() == false) {
      ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final Integer addressId = form.getData().getId();
              final PageParameters params = new PageParameters();
              params.add(AbstractEditPage.PARAMETER_KEY_ID, addressId);
              final AddressViewPage addressViewPage = new AddressViewPage(params);
              setResponsePage(addressViewPage);
            }
          }, getString("printView"));
      addContentMenuEntry(menu);

      final ContentMenuEntryPanel singleIcalExport = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final AddressDO address = form.getData();
              final String filename = "ProjectForge-" + address.getFullName() + "_"
                  + DateHelper.getDateAsFilenameSuffix(new Date()) + ".vcf";
              final StringWriter writer = new StringWriter();
              addressDao.exportVCard(new PrintWriter(writer), address);
              DownloadUtils.setUTF8CharacterEncoding(getResponse());
              DownloadUtils.setDownloadTarget(writer.toString().getBytes(), filename);
            }
          }, getString("address.book.vCardSingleExport"));
      addContentMenuEntry(singleIcalExport);

      if (configurationService.isTelephoneSystemUrlConfigured() == true) {
        menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Void>(ContentMenuEntryPanel.LINK_ID)
        {
          @Override
          public void onClick()
          {
            final Integer addressId = form.getData().getId();
            final PageParameters params = new PageParameters();
            params.add(PhoneCallPage.PARAMETER_KEY_ADDRESS_ID, addressId);
            setResponsePage(new PhoneCallPage(params));
          }
        }, getString("address.directCall.call"));
        addContentMenuEntry(menu);
      }
    }
  }

  @Override
  public void create()
  {
    if (cloneFlag == true) {
      super.updateAndNext();
      cloneFlag = false;
    } else {
      super.create();
    }
  }

  public boolean getCloneFlag()
  {
    return cloneFlag;
  }

  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    final AddressDO address = addressDao.getOrLoad(getData().getId());
    final PersonalAddressDO personalAddress = form.addressEditSupport.personalAddress;
    personalAddress.setAddress(address);
    personalAddressDao.setOwner(personalAddress, getUserId()); // Set current logged in user as owner.
    personalAddressDao.saveOrUpdate(personalAddress);
    return null;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData()
  {
    cloneFlag = true;
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    if (cloneFlag == true) {
      clonedAddress = new AddressDO();
      clonedAddress.copyValuesFrom(getData());
      final PersonalAddressDO personalAddress = new PersonalAddressDO();
      personalAddress.copyValuesFrom(form.addressEditSupport.personalAddress);
      personalAddress.setCreated(null);
      personalAddress.setLastUpdate(null);
      personalAddress.setDeleted(false);
      personalAddress.setId(null);
      personalAddress.setAddress(clonedAddress);

      clonedAddress.setId(null);
      clonedAddress.setUid(null);
      addressDao.save(clonedAddress);
      for (Map.Entry<String, JpaTabAttrBaseDO<AddressDO, Integer>> entry : getData().getAttrs().entrySet()) {
        clonedAddress.putAttribute(entry.getKey(), entry.getValue().getStringData());
      }
      addressDao.update(clonedAddress);
      form.addressEditSupport.personalAddress = personalAddress;
      Field data = null;
      try {
        data = AbstractEditForm.class.getDeclaredField("data");
        data.setAccessible(true);
        data.set(form, clonedAddress);
      } catch (IllegalAccessException | NoSuchFieldException e) {
        e.printStackTrace();
      }
      cloneFlag = false;
    }
    //Check addressbook changes
    if (getData().getId() != null) {
      AddressDO dbAddress = addressDao.internalGetById(getData().getId());
      AddressbookRight addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
      for (AddressbookDO dbAddressbook : dbAddress.getAddressbookList()) {
        //If user has no right for assigned addressbook, it could not be removed
        if (addressbookRight.hasSelectAccess(ThreadLocalUserContext.getUser(), dbAddressbook) == false
            && getData().getAddressbookList().contains(dbAddressbook) == false) {
          getData().getAddressbookList().add(dbAddressbook);
        }
      }
    }

    byte[] image = null;
    if (getData().getImageData() != null) {
      image = imageService.resizeImage(getData().getImageData());
    }
    getData().setImageDataPreview(image);
    return null;
  }

  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  @Override
  protected AddressEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AddressDO data)
  {
    return new AddressEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
