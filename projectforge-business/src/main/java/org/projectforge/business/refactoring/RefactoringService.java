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
