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
