package org.projectforge.microservices.address.repository;

import java.util.List;

import org.projectforge.jpa.model.address.Address;
import org.springframework.data.repository.CrudRepository;

public interface AddressRepository extends CrudRepository<Address, Integer>
{
  List<Address> findByName(String name);
}
