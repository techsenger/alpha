/*
 * Copyright 2018-2026 Pavel Castornii.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.techsenger.weaverbird.core.api.component;

import com.techsenger.toolkit.core.version.Version;

/**
 *
 * @author Pavel Castornii
 */
public interface ParentConfig {

    String getName();

    Version getVersion();

    /**
     * Returns the version match. Default value is {@link VersionMatch#ANY}.
     *
     * @return
     */
    VersionMatch getVersionMatch();

    /**
     * Returns whether this parent is optional. An optional parent that cannot be matched to a deployed
     * component does not block resolution/deployment of the owning component, unlike a required
     * (non-optional) parent. Default value is {@code false}.
     *
     * @return
     */
    boolean isOptional();
}
