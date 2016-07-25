package org.projectforge.microservices.address;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.projectforge.dto.address.AddressDTO;
import org.projectforge.dto.address.AddressMapper;
import org.projectforge.jpa.model.address.Address;
import org.projectforge.microservices.address.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A RESTFul controller for accessing address information.
 * 
 * @author Florian Blumenstein
 */
@RestController
public class AddressController
{

  protected Logger logger = Logger.getLogger(AddressController.class
      .getName());

  @Autowired
  private AddressRepository addressRepo;

  @Autowired
  private AddressMapper mapper;

  /**
   * Fetch an address with the specified address number.
   * 
   * @param addressNumber A numeric, 9 digit address number.
   * @return The address if found.
   * @throws AddressNotFoundException If the number is not recognised.
   */
  @RequestMapping("/address/{addressNumber}")
  public String byNumber(@PathVariable("addressNumber") String addressNumber)
  {

    logger.info("address-service byNumber() invoked: " + addressNumber);
    Address address = addressRepo.findOne(Integer.parseInt(addressNumber));
    logger.info("address-service byNumber() found: " + address);

    if (address == null)
      throw new AddressNotFoundException(addressNumber);
    else {
      return address.toString();
    }
  }

  /**
   * Fetch an address with the specified address number.
   * 
   * @param addressNumber A numeric, 9 digit address number.
   * @return The address if found.
   * @throws AddressNotFoundException If the number is not recognised.
   */
  @RequestMapping("/address/list/")
  public List<AddressDTO> list()
  {

    logger.info("address-service list() invoked.");
    Iterable<Address> addressIter = addressRepo.findAll();
    List<AddressDTO> addressDTOList = new ArrayList<>();
    for (Address address : addressIter) {
      addressDTOList.add(mapper.mapToDTO(address));
    }
    logger.info("address-service list() found size: " + addressDTOList.size());

    return addressDTOList;
  }

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
   * @param all If true and the user is member of the ProjectForge's group {@link ProjectForgeGroup#FINANCE_GROUP} or
   *          {@link ProjectForgeGroup#MARKETING_GROUP} the export contains all addresses instead of only favorite
   *          addresses.
   */
  //  @RequestMapping("/address/list")
  //  public Response getList(@RequestParam(value = "search", required = false) final String searchTerm,
  //      @RequestParam(value = "modifiedSince", required = false) final Long modifiedSince,
  //      @RequestParam(value = "all", required = false) final Boolean all)
  //  {
  //    final AddressFilter filter = new AddressFilter(new BaseSearchFilter());
  //    Date modifiedSinceDate = null;
  //    if (modifiedSince != null) {
  //      modifiedSinceDate = new Date(modifiedSince);
  //      filter.setModifiedSince(modifiedSinceDate);
  //    }
  //    filter.setSearchString(searchTerm);
  //    final List<AddressDO> list = addressService.find(filter);
  //
  //    boolean exportAll = false;
  //    if (BooleanUtils.isTrue(all) == true
  //        && accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
  //            ProjectForgeGroup.MARKETING_GROUP) == true) {
  //      exportAll = true;
  //    }
  //
  //    List<PersonalAddressDO> favorites = null;
  //    Set<Integer> favoritesSet = null;
  //    if (exportAll == false) {
  //      favorites = personalAddressDao.getList();
  //      favoritesSet = new HashSet<Integer>();
  //      if (favorites != null) {
  //        for (final PersonalAddressDO personalAddress : favorites) {
  //          if (personalAddress.isFavoriteCard() == true && personalAddress.isDeleted() == false) {
  //            favoritesSet.add(personalAddress.getAddressId());
  //          }
  //        }
  //      }
  //    }
  //    final List<AddressObject> result = new LinkedList<AddressObject>();
  //    final Set<Integer> alreadyExported = new HashSet<Integer>();
  //    if (list != null) {
  //      for (final AddressDO addressDO : list) {
  //        if (exportAll == false && favoritesSet.contains(addressDO.getId()) == false) {
  //          // Export only personal favorites due to data-protection.
  //          continue;
  //        }
  //        final AddressObject address = AddressDOConverter.getAddressObject(addressDO);
  //        result.add(address);
  //        alreadyExported.add(address.getId());
  //      }
  //    }
  //    if (exportAll == false && modifiedSinceDate != null) {
  //      // Add now personal address entries which were modified since the given date (deleted or added):
  //      for (final PersonalAddressDO personalAddress : favorites) {
  //        if (alreadyExported.contains(personalAddress.getAddressId()) == true) {
  //          // Already exported:
  //        }
  //        if (personalAddress.getLastUpdate() != null
  //            && personalAddress.getLastUpdate().before(modifiedSinceDate) == false) {
  //          final AddressDO addressDO = addressService.findById(personalAddress.getAddressId());
  //          final AddressObject address = AddressDOConverter.getAddressObject(addressDO);
  //          if (personalAddress.isFavorite() == false) {
  //            // This address was may-be removed by the user from the personal address book, so add this address as deleted to the result
  //            // list.
  //            address.setDeleted(true);
  //          }
  //          result.add(address);
  //        }
  //      }
  //    }
  //    @SuppressWarnings("unchecked")
  //    final List<AddressObject> uniqResult = (List<AddressObject>) CollectionUtils.select(result,
  //        PredicateUtils.uniquePredicate());
  //    final String json = JsonUtils.toJson(uniqResult);
  //    logger.info("Rest call finished (" + result.size() + " addresses)...");
  //    return Response.ok(json).build();
  //  }

  /**
   * Fetch address with the specified lastname.
   * 
   * @param lastname
   * @return A non-null, non-empty set of address.
   * @throws AddressNotFoundException If there are no matches at all.
   */
  @RequestMapping("/acddress/lastname/{lastname}")
  public List<Address> byLastname(@PathVariable("lastname") String lastname)
  {
    logger.info("address-service byLastname() invoked: "
        + addressRepo.getClass().getName() + " for "
        + lastname);

    List<Address> address = addressRepo
        .findByName(lastname);
    logger.info("address-service byLastname() found: " + address);

    if (address == null || address.size() == 0)
      throw new AddressNotFoundException(lastname);
    else {
      return address;
    }
  }
}
