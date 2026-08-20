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

package com.techsenger.weaverbird.gui.console;

import atlantafx.base.theme.Styles;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.toolkit.fx.Spacer;
import com.techsenger.weaverbird.gui.session.AbstractSessionToolBarFxView;
import com.techsenger.weaverbird.gui.style.WeaverbirdIcons;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Pavel Castornii
 */
public class ConsoleToolBarFxView<P extends ConsoleToolBarPresenter<?>> extends AbstractSessionToolBarFxView<P>
        implements ConsoleToolBarView {

    private final Button clearButton = new Button(null, new FontIconView(WeaverbirdIcons.CLEAR));

    private final Button copyButton = new Button(null, new FontIconView(WeaverbirdIcons.COPY));

    private final Button pasteButton = new Button(null, new FontIconView(WeaverbirdIcons.PASTE));

    @Override
    public void requestFocus() {

    }

    @Override
    public void updateCopyDisabled(boolean disabled) {
        this.copyButton.setDisable(disabled);
    }

    @Override
    public void updatePasteDisabled(boolean disabled) {
        this.pasteButton.setDisable(disabled);
    }

    @Override
    protected void build() {
        super.build();
        clearButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);
        clearButton.setTooltip(new Tooltip("Clear"));
        copyButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);
        copyButton.setTooltip(new Tooltip("Copy"));
        pasteButton.getStyleClass().addAll(Styles.FLAT, StyleClasses.SIZE_L);
        pasteButton.setTooltip(new Tooltip("Paste"));

        getNode().getItems().addAll(clearButton, new Separator(Orientation.VERTICAL), copyButton, pasteButton,
                new Spacer(Orientation.HORIZONTAL), getSessionLabel(), getSessionComboBox(), getRefreshButton());
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        clearButton.setOnAction(e -> getPresenter().onClear());
        copyButton.setOnAction(e -> getPresenter().onCopy());
        pasteButton.setOnAction(e -> getPresenter().onPaste());
    }
}
