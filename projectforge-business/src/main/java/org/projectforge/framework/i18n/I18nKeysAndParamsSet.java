package org.projectforge.framework.i18n;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Container holding Set of I18n Keys and Params - Objects and logic hereupon.
 *
 * @author Matthias Altmann (m.altmann@micromata.de)
 */
public class I18nKeysAndParamsSet extends HashSet
{

  /**
   * Checks if I18nKeyAndParams object is in the set.
   *
   * @param i18nKeyAndParamsToCheck the 18nkeyandparams to check
   * @return true if inside, false if not.
   */
  public boolean isInSet(I18nKeyAndParams i18nKeyAndParamsToCheck)
  {
    for (Iterator<I18nKeyAndParams> iterator = this.iterator(); iterator.hasNext(); ) {
      I18nKeyAndParams i18nKeyAndParamsInSet = iterator.next();
      if (i18nKeyAndParamsInSet.equals(i18nKeyAndParamsToCheck) == true) {
        return true;
      }
    }
    return false;
  }
}
