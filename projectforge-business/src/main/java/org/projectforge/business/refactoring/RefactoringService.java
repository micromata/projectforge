/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.refactoring;

public interface RefactoringService
{

  /**
   * Returns the new package name for a given full qualified class name
   * 
   * @param fullQualifiedClassName The full qualified name of the class to search
   * @return The new package name, if it was refactored or null, if not found
   */
  String getNewPackageNameForFullQualifiedClass(String fullQualifiedClassName);

  /**
   * Returns the new package name for a given class name without package name
   * 
   * @param className The name of the class to search without package name
   * @return The new package name, if it was refactored or null, if not found
   */
  String getNewPackageNameForClass(String className);

  /**
   * Parse the packe name of an full qualified class name
   * 
   * @param fullClassName E.g. org.projectforge.framework.calendar.WeekHolder
   * @return the packe name (e.g. org.projectforge.framework.calendar)
   */
  String getPackageName(String fullClassName);

}
