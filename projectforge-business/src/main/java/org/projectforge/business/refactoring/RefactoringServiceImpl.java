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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RefactoringServiceImpl implements RefactoringService
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RefactoringServiceImpl.class);

  Map<String, String> refactoredClasses;

  @PostConstruct
  public void init()
  {
    try {
      ClassPathResource r = new ClassPathResource("refactoringChanges.properties");
      Properties p = new Properties();
      p.load(r.getInputStream());
      Stream<Entry<Object, Object>> stream = p.entrySet().stream();
      refactoredClasses = stream.collect(Collectors.toMap(
          e -> String.valueOf(e.getKey()),
          e -> String.valueOf(e.getValue())));
    } catch (IOException e) {
      log.error("Error while reading refactoringChanges.properties. ", e);
    }
  }

  @Override
  public String getNewPackageNameForFullQualifiedClass(String fullQualifiedSourceClassName)
  {
    String refactoredClassName = refactoredClasses.get(fullQualifiedSourceClassName);
    if (refactoredClassName != null) {
      return getPackageName(refactoredClassName);
    }
    return null;
  }

  @Override
  public String getPackageName(String fullClassName)
  {
    int index = fullClassName.lastIndexOf(".");
    String result = fullClassName.substring(0, index);
    return result;
  }

  @Override
  public String getNewPackageNameForClass(String className)
  {
    for (String fullClassName : refactoredClasses.keySet()) {
      String newPackageClassName = refactoredClasses.get(fullClassName);
      if (newPackageClassName != null) {
        String[] parts = newPackageClassName.split("\\.");
        if (parts.length > 0 && parts[parts.length - 1].equals(className)) {
          return newPackageClassName;
        }
      }
    }
    return null;
  }

}
