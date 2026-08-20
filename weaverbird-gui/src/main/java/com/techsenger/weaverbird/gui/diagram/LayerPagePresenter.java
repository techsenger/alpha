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

package com.techsenger.weaverbird.gui.diagram;

import com.techsenger.patternfx.mvp.ComponentDescriptor;
import com.techsenger.shellfx.core.page.AbstractPagePresenter;
import com.techsenger.weaverbird.gui.WeaverbirdComponents;

/**
 *
 * @author Pavel Castornii
 */
public class LayerPagePresenter<V extends LayerPageView> extends AbstractPagePresenter<V> implements LayerPagePort {

    private final LayerConfig layer;

    public LayerPagePresenter(V view, LayerPageParams params) {
        super(view, params);
        this.layer = params.getLayer();
    }

    @Override
    public void reset() {
        getView().refreshModules();
        getView().clearSelectAll();
    }

    @Override
    protected ComponentDescriptor createDescriptor() {
        return new ComponentDescriptor(WeaverbirdComponents.LAYER_PAGE);
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        getView().showLayer(layer);
    }
}
