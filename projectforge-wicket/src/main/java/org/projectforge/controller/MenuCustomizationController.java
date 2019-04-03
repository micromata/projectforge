package org.projectforge.controller;


import lombok.extern.slf4j.Slf4j;
import org.projectforge.menu.Menu;
import org.projectforge.menu.builder.FavoritesMenuCreator;
import org.projectforge.menu.builder.FavoritesMenuReaderWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

// TODO This controller should be moved to a controller package in the business-module, but for now it has
// to much dependencies on wicket to do so. This could be solved by reimplementing the UserPreference stuff
@Controller
@RequestMapping("/secure/menucustomization")
@Slf4j
public class MenuCustomizationController
{

  @Autowired
  private FavoritesMenuCreator favoritesMenuCreator;

  @PostMapping("/customize")
  @ResponseBody
  public String customize(@RequestParam("configuration") String configuration) {
    log.debug(configuration);
    Menu menu = favoritesMenuCreator.read(configuration);
    FavoritesMenuReaderWriter.Companion.storeAsUserPref(menu);
    return "success";
  }
}
