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

import atlantafx.base.theme.Styles;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.toolkit.fx.Spacer;
import com.techsenger.weaverbird.gui.session.AbstractSessionToolBarFxView;
import com.techsenger.weaverbird.gui.style.WeaverbirdIcons;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Pavel Castornii
 */
public class DiagramToolBarFxView<P extends DiagramToolBarPresenter<?>> extends AbstractSessionToolBarFxView<P>
        implements DiagramToolBarView {

    private final Button layerDiagramButton = new Button(null, new FontIconView(WeaverbirdIcons.LAYER_DIAGRAMS));

    private final ComboBox<String> zoomLevelComboBox = new ComboBox<>();

    private final Button zoomOutButton = new Button(null, new FontIconView(WeaverbirdIcons.ZOOM_OUT));

    private final Button zoomInButton = new Button(null, new FontIconView(WeaverbirdIcons.ZOOM_IN));

    @Override
    public void setZoomLevels(List<String> levels) {
        zoomLevelComboBox.setItems(FXCollections.observableArrayList(levels));
    }

    @Override
    public void setZoomLevel(String level) {
        zoomLevelComboBox.getSelectionModel().select(level);
    }

    @Override
    public void requestFocus() {

    }

    @Override
    protected void build() {
        super.build();
        layerDiagramButton.setTooltip(new Tooltip("Layer Diagram"));
        layerDiagramButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);
        zoomLevelComboBox.getStyleClass().add(Styles.DENSE);
        zoomInButton.setTooltip(new Tooltip("Zoom In"));
        zoomInButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);
        zoomOutButton.setTooltip(new Tooltip("Zoom Out"));
        zoomOutButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);

        getNode().getItems().addAll(layerDiagramButton, new Separator(Orientation.VERTICAL),
            zoomOutButton, zoomLevelComboBox, zoomInButton, new Spacer(Orientation.HORIZONTAL),
            getSessionLabel(), getSessionComboBox(), getRefreshButton());
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        zoomLevelComboBox.valueProperty().addListener((ov, oldV, newV) -> getPresenter().onZoomLevelChanged(newV));
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        layerDiagramButton.setOnAction(e -> getPresenter().onLayerDiagram());
        zoomOutButton.setOnAction(e -> getPresenter().onZoomOut());
        zoomInButton.setOnAction(e -> getPresenter().onZoomIn());
    }

    protected Button getLayerDiagramButton() {
        return layerDiagramButton;
    }

    protected ComboBox<String> getZoomLevelComboBox() {
        return zoomLevelComboBox;
    }

    protected Button getZoomOutButton() {
        return zoomOutButton;
    }

    protected Button getZoomInButton() {
        return zoomInButton;
    }
}
