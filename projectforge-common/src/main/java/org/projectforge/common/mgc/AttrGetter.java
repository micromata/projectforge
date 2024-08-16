//
// Copyright (C) 2010-2016 Micromata GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.projectforge.common.mgc;

/**
 * Interface to retrieve a property from a bean.
 *
 * @author roger@micromata.de
 *
 */
@FunctionalInterface
public interface AttrGetter<BEAN, VAL>
{

  /**
   * get the attr.
   *
   * @param bean the bean
   * @return the val
   */
  VAL get(BEAN bean);
}
