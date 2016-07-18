package org.projectforge.packagecompare;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageComparator
{
  private static String IGNORE_FILE = ".DS_Store";

  private static String JAVA_PATTERN = ".java";

  private static String MAVEN_PATTERN = "src/main/java";

  private static final String MAIN_ROOT_PATH = "/Users/blumenstein/Projekte/Projectforge/git/";

  private static final String OLD_ROOT_PATH = MAIN_ROOT_PATH + "projectforge-webapp/" + MAVEN_PATTERN;

  private static final String NEW_ROOT_PATH_BUSINESS = MAIN_ROOT_PATH
      + "projectforge/projectforge-business/" + MAVEN_PATTERN;

  private static final String NEW_ROOT_PATH_WICKET = MAIN_ROOT_PATH + "projectforge/projectforge-wicket/"
      + MAVEN_PATTERN;

  private static String[] plugins = { "org.projectforge.plugins.skillmatrix", "org.projectforge.plugins.banking",
      "org.projectforge.plugins.crm", "org.projectforge.plugins.licensemanagement",
      "org.projectforge.plugins.liquidityplanning", "org.projectforge.plugins.marketing",
      "org.projectforge.plugins.memo", "org.projectforge.plugins.poll", "org.projectforge.plugins.todo" };

  private static final String NEW_ROOT_PATH_PLUGINS = MAIN_ROOT_PATH + "projectforge/plugins/";

  private static Map<String, String> oldStructure = new HashMap<>();

  private static Map<String, String> newStructure = new HashMap<>();

  public static void main(String[] args)
  {
    System.out.println("Start with old root path: " + OLD_ROOT_PATH);

    File rootOld = new File(OLD_ROOT_PATH);
    write(rootOld, oldStructure, OLD_ROOT_PATH);
    for (String fileName : oldStructure.keySet()) {
      System.out.println(oldStructure.get(fileName) + "." + fileName);
    }
    System.out.println("Anzahl Gefundener Klasse (alt): " + oldStructure.size());

    File rootNewBusiness = new File(NEW_ROOT_PATH_BUSINESS);
    write(rootNewBusiness, newStructure,
        NEW_ROOT_PATH_BUSINESS);
    File rootNewWicket = new File(NEW_ROOT_PATH_WICKET);
    write(rootNewWicket, newStructure,
        NEW_ROOT_PATH_WICKET);
    for (String fileName : newStructure.keySet()) {
      System.out.println(newStructure.get(fileName) + "." + fileName);
    }
    for (String pluginName : plugins) {
      File rootNewPlugin = new File(NEW_ROOT_PATH_PLUGINS + pluginName);
      write(rootNewPlugin, newStructure,
          NEW_ROOT_PATH_PLUGINS + pluginName + "/" + MAVEN_PATTERN);
      for (String fileName : newStructure.keySet()) {
        System.out.println(newStructure.get(fileName) + "." + fileName);
      }
    }
    System.out.println("Anzahl Gefundener Klasse (neu): " + newStructure.size());

    System.out.println("Anzahl Gefundener Klasse alt: " + oldStructure.size());
    System.out.println("Anzahl Gefundener Klasse neu: " + newStructure.size());

    writePropertiesFile();
  }

  private static void writePropertiesFile()
  {
    List<String> diff = new ArrayList<>();
    for (String fileNameOld : oldStructure.keySet()) {
      String oldPackage = oldStructure.get(fileNameOld) + "." + fileNameOld;
      if (newStructure.get(fileNameOld) != null) {
        String newPackage = newStructure.get(fileNameOld) + "." + fileNameOld;
        if (newPackage.equals(oldPackage) == false) {
          System.out.println(oldPackage + "=" + newPackage);
        }
      } else {
        diff.add(fileNameOld);
      }
    }
    for (String s : diff) {
      System.out.println("Klasse in new structure not found: " + s);
    }
    for (String s : newStructure.keySet()) {
      if (oldStructure.get(s) == null) {
        System.out.println("Klasse in old structure not found: " + s);
      }
    }
  }

  private static void write(File parent, Map<String, String> structureMap, String replacer)
  {
    for (String childString : parent.list()) {
      File child = new File(parent.getAbsolutePath() + "/" + childString);
      if (child.getName().equals(IGNORE_FILE) == false) {
        if (child.isDirectory()) {
          write(child, structureMap, replacer);
        } else {
          if (child.getName().endsWith(JAVA_PATTERN) == true) {
            structureMap.put(cleanFilename(child.getName()), cleanFolder(child.getParent(), replacer));
          }
        }
      }
    }
  }

  private static String cleanFolder(String source, String replacer)
  {
    String clean = source
        .replace(replacer, "");
    String target = clean.replaceAll("/", ".").substring(1);

    return target;
  }

  private static String cleanFilename(String source)
  {
    String clean = source.replace(".java", "");
    return clean;
  }

}
