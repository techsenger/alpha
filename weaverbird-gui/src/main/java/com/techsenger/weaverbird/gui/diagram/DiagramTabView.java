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

import com.techsenger.shellfx.core.tab.HostTabView;
import com.techsenger.weaverbird.core.api.model.ComponentLayerModel;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.List;
import javafx.scene.image.Image;

/**
 *
 * @author Pavel Castornii
 */
public interface DiagramTabView extends HostTabView {

    interface Composer extends HostTabView.Composer {

        void setClient(ClientService client);

        void setSession(ClientSession session);

        LayerDialogPort openLayerDialog(List<ComponentLayerModel> layerModels, List<LayerConfig> previousLayerConfigs);
    }

    @Override
    Composer getComposer();

    void updateDiagram(Image diagram);

    void updateDiagramSize(double width, double height);
}
