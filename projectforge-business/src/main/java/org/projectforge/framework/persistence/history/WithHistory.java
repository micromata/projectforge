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

package org.projectforge.framework.persistence.history;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Mark entity, which should be has History.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface WithHistory {

    /**
     * Which code should be used to retrieve properties.
     *
     * @return the class ? extends history property provider []
     */
    //Class<? extends HistoryPropertyProvider>[] propertyProvider() default { DefaultHistoryPropertyProvider.class };

    /**
     * List of properties names not create history entries.
     *
     * @return the string[]
     */
    String[] noHistoryProperties() default {};

    /**
     * List of properties, which should make history, even it is in noHistoryProperties.
     *
     * @return the string[]
     */
    String[] forceHistoryProperties() default {};

    /**
     * A class, which have nested history entities.
     *
     * @return the nested entities
     */
    Class<?>[] nestedEntities() default {};

}
