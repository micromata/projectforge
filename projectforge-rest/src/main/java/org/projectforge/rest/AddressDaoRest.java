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

package org.projectforge.rest;

import static org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressFilter;
import org.projectforge.business.address.AddressStatus;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.address.ContactStatus;
import org.projectforge.business.address.FormOfAddress;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.model.rest.AddressObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.converter.AddressDOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * REST-Schnittstelle für {@link AddressDao}
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.ADDRESS)
public class AddressDaoRest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressDaoRest.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private PersonalAddressDao personalAddressDao;

  @Autowired
  private AddressbookDao addressbookDao;

  /**
   * Rest-Call for {@link AddressDao#getFavoriteVCards()}. <br/>
   * If modifiedSince is given then only those addresses will be returned:
   * <ol>
   * <li>The address was changed after the given modifiedSince date, or</li>
   * <li>the address was added to the user's personal address book after the given modifiedSince date, or</li>
   * <li>the address was removed from the user's personal address book after the given modifiedSince date.</li>
   * </ol>
   *
   * @param searchTerm
   * @param modifiedSince milliseconds since 1970 (UTC)
   * @param all           If true and the user is member of the ProjectForge's group {@link ProjectForgeGroup#FINANCE_GROUP} or
   *                      {@link ProjectForgeGroup#MARKETING_GROUP} the export contains all addresses instead of only favorite
   *                      addresses.
   */
  @GET
  @Path(RestPaths.LIST)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList(@QueryParam("search") final String searchTerm,
      @QueryParam("modifiedSince") final Long modifiedSince,
      @QueryParam("all") final Boolean all,
      @QueryParam("disableImageData") final Boolean disableImageData,
      @QueryParam("disableVCardData") final Boolean disableVCardData)
  {
    final AddressFilter filter = new AddressFilter(new BaseSearchFilter());
    Date modifiedSinceDate = null;
    if (modifiedSince != null) {
      modifiedSinceDate = new Date(modifiedSince);
      filter.setModifiedSince(modifiedSinceDate);
    }
    filter.setSearchString(searchTerm);
    final List<AddressDO> list = addressDao.getList(filter);

    boolean exportAll = false;
    if (BooleanUtils.isTrue(all) == true
        && accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.MARKETING_GROUP) == true) {
      exportAll = true;
    }

    List<PersonalAddressDO> favorites = null;
    Set<Integer> favoritesSet = null;
    if (exportAll == false) {
      favorites = personalAddressDao.getList();
      favoritesSet = new HashSet<Integer>();
      if (favorites != null) {
        for (final PersonalAddressDO personalAddress : favorites) {
          if (personalAddress.isFavoriteCard() == true && personalAddress.isDeleted() == false) {
            favoritesSet.add(personalAddress.getAddressId());
          }
        }
      }
    }
    final List<AddressObject> result = new LinkedList<AddressObject>();
    if (list != null) {
      for (final AddressDO addressDO : list) {
        if (exportAll == false && favoritesSet.contains(addressDO.getId()) == false) {
          // Export only personal favorites due to data-protection.
          continue;
        }
        final AddressObject address = AddressDOConverter.getAddressObject(addressDao, addressDO,
            BooleanUtils.isTrue(disableImageData), BooleanUtils.isTrue(disableVCardData));
        result.add(address);
      }
    }
    if (exportAll == false && modifiedSinceDate != null) {
      // Add now personal address entries which were modified since the given date (deleted or added):
      for (final PersonalAddressDO personalAddress : favorites) {
        if (personalAddress.getLastUpdate() != null
            && personalAddress.getLastUpdate().before(modifiedSinceDate) == false) {
          final AddressDO addressDO = addressDao.getById(personalAddress.getAddressId());
          final AddressObject address = AddressDOConverter.getAddressObject(addressDao, addressDO,
              BooleanUtils.isTrue(disableImageData), BooleanUtils.isTrue(disableVCardData));
          if (personalAddress.isFavorite() == false) {
            // This address was may-be removed by the user from the personal address book, so add this address as deleted to the result
            // list.
            address.setDeleted(true);
          }
          result.add(address);
        }
      }
    }
    @SuppressWarnings("unchecked")
    final List<AddressObject> uniqResult = (List<AddressObject>) CollectionUtils.select(result,
        PredicateUtils.uniquePredicate());
    final String json = JsonUtils.toJson(uniqResult);
    log.info("Rest call finished (" + result.size() + " addresses)...");
    return Response.ok(json).build();
  }

  @PUT
  @Path(RestPaths.SAVE_OR_UDATE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveOrUpdateAddressObject(final AddressObject addressObject)
  {
    String uid = addressObject.getUid() != null ? addressObject.getUid().replace("urn:uuid:", "") : UUID.randomUUID().toString();
    addressObject.setUid(uid);
    AddressDO addressDORequest = AddressDOConverter.getAddressDO(addressObject);
    AddressDO addressDOOrig = null;
    boolean isNew = false;
    try {
      addressDOOrig = addressDao.findByUid(addressObject.getUid());
    } catch (javax.persistence.NoResultException e) {
      log.info("No address with given uid found: " + uid);
      log.info("Continoue creating new address.");
    }

    if (addressDOOrig != null) {
      //Metadata
      addressDORequest.setId(addressDOOrig.getId());
      addressDORequest.setCreated(addressDOOrig.getCreated());
      //Data, which is not in vcard
      addressDORequest.setAddressStatus(addressDOOrig.getAddressStatus());
      addressDORequest.setContactStatus(addressDOOrig.getContactStatus());
      addressDORequest.setForm(addressDOOrig.getForm());
      addressDORequest.setCommunicationLanguage(addressDOOrig.getCommunicationLanguage());

      addressDORequest.setPostalAddressText(addressDOOrig.getPostalAddressText());
      addressDORequest.setPostalCity(addressDOOrig.getPostalCity());
      addressDORequest.setPostalCountry(addressDOOrig.getPostalCountry());
      addressDORequest.setPostalZipCode(addressDOOrig.getPostalZipCode());
      addressDORequest.setPostalState(addressDOOrig.getPostalState());

      addressDORequest.setPublicKey(addressDOOrig.getPublicKey());
      addressDORequest.setFingerprint(addressDOOrig.getFingerprint());
      //Addressbooks
      addressDORequest.setAddressbookList(addressDOOrig.getAddressbookList());
    } else {
      addressDORequest.setAddressStatus(AddressStatus.UPTODATE);
      addressDORequest.setContactStatus(ContactStatus.ACTIVE);
      isNew = true;
    }

    if (addressDORequest.getAddressbookList() == null || addressDORequest.getAddressbookList().size() < 1) {
      Set<AddressbookDO> addressbooks = new HashSet<>();
      addressbooks.add(addressbookDao.getGlobalAddressbook());
      addressDORequest.setAddressbookList(addressbooks);
      addressDORequest.setForm(FormOfAddress.UNKNOWN);
    }

    addressDao.saveOrUpdate(addressDORequest);

    AddressDO dbAddress = addressDao.findByUid(uid);

    if (isNew) {
      PersonalAddressDO personalAddress = null;
      personalAddress = new PersonalAddressDO();
      personalAddress.setAddress(dbAddress);
      personalAddress.setFavoriteCard(true);
      personalAddressDao.setOwner(personalAddress, getUserId());
      personalAddressDao.saveOrUpdate(personalAddress);
    }

    final String json = JsonUtils.toJson(AddressDOConverter.getAddressObject(addressDao, dbAddress,
        false, true));
    log.info("Save or update address REST call finished.");
    return Response.ok(json).build();
  }

  @DELETE
  @Path(RestPaths.DELETE)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response removeFavoriteAddressObject(final AddressObject addressObject)
  {
    String uid = addressObject.getUid() != null ? addressObject.getUid().replace("urn:uuid:", "") : UUID.randomUUID().toString();
    addressObject.setUid(uid);
    AddressDO addressDOOrig = null;
    try {
      addressDOOrig = addressDao.findByUid(addressObject.getUid());
    } catch (javax.persistence.NoResultException e) {
      log.info("No address with given uid found: " + uid);
      log.info("Serving error response.");
    }
    if (addressDOOrig == null) {
      return Response.serverError().build();
    }
    PersonalAddressDO personalAddress = personalAddressDao.getByAddressId(addressDOOrig.getId());
    if (personalAddress != null) {
      personalAddress.setFavoriteCard(false);
      personalAddressDao.saveOrUpdate(personalAddress);
    }
    return Response.ok().build();
  }
}
