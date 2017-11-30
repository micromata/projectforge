package org.projectforge.controller;


import lombok.extern.slf4j.Slf4j;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.web.FavoritesMenu;
import org.projectforge.web.MenuBuilder;
import org.projectforge.web.MenuItemRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

// TODO This controller should be moved to a controller package in the business-module, but for now it has
// to much dependencies on wicket to do so. This could be solved by reimplementing the UserPreference stuff
@Controller
@RequestMapping("/secure/menucustomization")
@Slf4j
public class MenuCustomizationController
{

  @Autowired
  private MenuItemRegistry menuItemRegistry;

  @Autowired
  private MenuBuilder menuBilder;

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserRightService userRights;




  @PostMapping("/customize")
  @ResponseBody
  public String customize(@RequestParam("configuration") String configuration) {
    log.debug(configuration);
    FavoritesMenu favoritesMenu = FavoritesMenu.get(menuItemRegistry, menuBilder, accessChecker, userRights);

    favoritesMenu.readFromXml(configuration);
    favoritesMenu.storeAsUserPref();
    return "success";
  }
}
