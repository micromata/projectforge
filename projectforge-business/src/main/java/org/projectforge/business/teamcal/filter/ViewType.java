/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.projectforge.business.teamcal.filter;

public enum ViewType
{
  MONTH("month"), BASIC_WEEK("basicWeek"), BASIC_DAY("basicDay"), AGENDA_WEEK("agendaWeek"), AGENDA_DAY("agendaDay");

  private final String code;

  private ViewType(final String code)
  {
    this.code = code;
  }

  public static ViewType forCode(final String code)
  {
    for (final ViewType type : values()) {
      if (type.code.equals(code))
        return type;
    }
    throw new IllegalStateException("Invalid view type code: " + code);
  }

  /**
   * @author Micromata
   * @return the code
   */
  public String getCode()
  {
    return code;
  }
}
