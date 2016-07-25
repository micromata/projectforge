package org.projectforge.dto.address;

import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.projectforge.jpa.model.address.Address;
import org.springframework.stereotype.Service;

@Service
public class AddressMapper
{
  public AddressDTO mapToDTO(Address address)
  {
    Mapper mapper = new DozerBeanMapper();
    AddressDTO destObject = mapper.map(address, AddressDTO.class);
    return destObject;
  }

}
