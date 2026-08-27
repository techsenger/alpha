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

package com.techsenger.weaverbird.core.impl.component;

import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.VersionMatch;
import com.techsenger.toolkit.core.version.Version;

/**
 *
 * @author Pavel Castornii
 */
public final class DefaultParentConfig implements ParentConfig {

    private final String name;

    private final Version version;

    private final VersionMatch versionMatch;

    private final boolean optional;

    public DefaultParentConfig(String name, Version version, VersionMatch versionMatch, boolean optional) {
        this.name = name;
        this.version = version;
        this.versionMatch = versionMatch;
        this.optional = optional;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public VersionMatch getVersionMatch() {
        return versionMatch;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }
}

