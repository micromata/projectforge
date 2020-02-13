package org.projectforge.caldav.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.projectforge.caldav.cache.UserCache;
import org.projectforge.caldav.config.ApplicationContextProvider;
import org.projectforge.caldav.model.User;
import org.projectforge.caldav.model.UsersHome;
import org.projectforge.caldav.repo.UserRepository;
import org.projectforge.caldav.rest.AuthenticateRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.annotations.AccessControlList;
import io.milton.annotations.Authenticate;
import io.milton.annotations.ChildrenOf;
import io.milton.annotations.Users;
import io.milton.resource.AccessControlledResource;

/**
 * Created by blumenstein on 21.11.16.
 */
public class BaseDavController
{
  private static Logger log = LoggerFactory.getLogger(BaseDavController.class);

  private AuthenticateRest authenticateRest;

  private UserRepository userRepo;

  private UserCache userCache;

  UsersHome usersHome;

  @AccessControlList
  public List<AccessControlledResource.Priviledge> getUserPrivs(User target, User currentUser)
  {
    List<AccessControlledResource.Priviledge> result = new ArrayList<>();
    if (target != null && currentUser != null && currentUser.getPk().equals(target.getPk())) {
      result.add(AccessControlledResource.Priviledge.ALL);
    } else {
      return AccessControlledResource.NONE;
    }
    return result;
  }

  @ChildrenOf
  @Users
  public Collection<User> getUsers(UsersHome usersHome)
  {
    return getUserRepository().findAllActive();
  }

  @Authenticate
  public Boolean authenticate(User user, String requestedPassword)
  {
    return getUserCache().isUserAuthenticationValid(user) || getAuthenticateRest().authenticate(user, requestedPassword);
  }

  UserCache getUserCache()
  {
    if (userCache == null) {
      userCache = ApplicationContextProvider.getApplicationContext().getBean(UserCache.class);
    }
    return userCache;
  }

  private AuthenticateRest getAuthenticateRest()
  {
    if (authenticateRest == null) {
      authenticateRest = ApplicationContextProvider.getApplicationContext().getBean(AuthenticateRest.class);
    }
    return authenticateRest;
  }

  private UserRepository getUserRepository()
  {
    if (userRepo == null) {
      userRepo = ApplicationContextProvider.getApplicationContext().getBean(UserRepository.class);
    }
    return userRepo;
  }
}
