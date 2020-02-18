package org.projectforge.caldav.repo;

import java.util.List;

import org.projectforge.caldav.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long>
{
  @Query("SELECT u FROM org.projectforge.caldav.model.User u WHERE u.deleted = false")
  List<User> findAllActive();
}
