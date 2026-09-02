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

import com.techsenger.weaverbird.gui.session.AbstractSessionToolBarPresenter;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.List;
import java.util.Objects;
import javafx.collections.FXCollections;

/**
 *
 * @author Pavel Castornii
 */
public class DiagramToolBarPresenter<V extends DiagramToolBarView> extends AbstractSessionToolBarPresenter<V> {

    private final DiagramToolBarAwarePort toolBarAware;

    private final List<String> zoomLevels = FXCollections.observableArrayList(List.of(
            "33%", "50%", "67%", "80%", "90%",
            "100%", "110%", "120%", "133%", "150%",
            "170%", "200%", "300%", "400%", "500%"));

    private String zoomLevel;

    public DiagramToolBarPresenter(V view, DiagramToolBarParams params) {
        super(view, params);
        this.toolBarAware = params.getToolBarAware();
    }

    public List<String> getZoomLevels() {
        return zoomLevels;
    }

    public String getZoomLevel() {
        return zoomLevel;
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        getView().updateZoomLevels(zoomLevels);
        setZoomLevel("100%");
    }

    protected void onLayerDiagram() {
        this.toolBarAware.onLayerDiagram();
    }

    @Override
    protected void onSessionChanged(ClientSession session) {
        super.onSessionChanged(session);
        this.toolBarAware.onSessionChanged(session);
    }

    protected void onZoomOut() {
        var index = this.zoomLevels.indexOf(this.zoomLevel);
        index--;
        if (index >= 0) {
            setZoomLevel(this.zoomLevels.get(index));
        }
    }

    void onZoomIn() {
        var index = this.zoomLevels.indexOf(this.zoomLevel);
        index++;
        if (index < this.zoomLevels.size()) {
            setZoomLevel(this.zoomLevels.get(index));
        }
    }

    protected void onZoomLevelChanged(String level) {
        this.zoomLevel = level;
        var l = zoomLevel.substring(0, zoomLevel.length() - 1);
        this.toolBarAware.onZoomLevelChanged(Integer.valueOf(l));
    }

    protected void setZoomLevel(String zoomLevel) {
        if (Objects.equals(this.zoomLevel, zoomLevel)) {
            return;
        }
        this.zoomLevel = zoomLevel;
        getView().updateZoomLevel(zoomLevel);
    }
}
