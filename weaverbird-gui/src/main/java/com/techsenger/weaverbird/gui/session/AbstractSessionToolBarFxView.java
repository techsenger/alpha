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

package com.techsenger.weaverbird.gui.session;

import atlantafx.base.theme.Styles;
import com.techsenger.shellfx.core.area.AbstractAreaFxView;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.weaverbird.gui.style.WeaverbirdIcons;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 *
 * @author Pavel Castornii
 */
public abstract class AbstractSessionToolBarFxView<P extends AbstractSessionToolBarPresenter<?>>
        extends AbstractAreaFxView<P> implements SessionToolBarView {

    private final Label sessionLabel = new Label("Session");

    private final ComboBox<ClientSession> sessionComboBox = new ComboBox<>();

    private final Button refreshButton = new Button(null, new FontIconView(WeaverbirdIcons.REFRESH));

    private final ToolBar toolBar = new ToolBar();

    @Override
    public void setSessions(List<ClientSession> sessions) {
        sessionComboBox.setItems(FXCollections.observableArrayList(sessions));
    }

    @Override
    public void setSession(ClientSession session) {
        sessionComboBox.getSelectionModel().select(session);
    }

    @Override
    public ToolBar getNode() {
        return this.toolBar;
    }

    @Override
    protected void build() {
        super.build();
        sessionComboBox.getStyleClass().add(Styles.DENSE);
        sessionComboBox.setMaxWidth(Double.MAX_VALUE);
        sessionComboBox.setCellFactory(lv -> new SessionListCell<ClientSession>());
        sessionComboBox.setButtonCell(new SessionListCell<ClientSession>());
        sessionComboBox.getStyleClass().add("session");
        HBox.setHgrow(sessionComboBox, Priority.ALWAYS);
        refreshButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);

        toolBar.getStyleClass().add(StyleClasses.BLEND);
        var css = AbstractSessionToolBarFxView.class.getResource("tool-bar.css").toExternalForm();
        toolBar.getStylesheets().add(css);
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        refreshButton.setOnAction(e -> getPresenter().onRefresh());
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        sessionComboBox.valueProperty().addListener((ov, oldV, newV) -> getPresenter().onSessionChanged(newV));
    }

    protected Label getSessionLabel() {
        return sessionLabel;
    }

    protected ComboBox<ClientSession> getSessionComboBox() {
        return sessionComboBox;
    }

    protected Button getRefreshButton() {
        return refreshButton;
    }
}
