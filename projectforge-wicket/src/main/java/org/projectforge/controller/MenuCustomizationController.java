/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.controller;


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
@RequestMapping("/rs/menucustomization")
public class MenuCustomizationController
{

  @Autowired
  private FavoritesMenuCreator favoritesMenuCreator;

  @PostMapping("/customize")
  @ResponseBody
  public String customize(@RequestParam("configuration") String configuration) {
    Menu menu = favoritesMenuCreator.read(configuration);
    FavoritesMenuReaderWriter.Companion.storeAsUserPref(menu);
    return "success";
  }
}
