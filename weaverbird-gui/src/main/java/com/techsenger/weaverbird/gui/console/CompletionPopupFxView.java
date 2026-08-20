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

import com.techsenger.shellfx.core.popup.AbstractPopupFxView;
import com.techsenger.toolkit.fx.utils.ListViewUtils;
import com.techsenger.toolkit.fx.utils.NodeUtils;
import com.techsenger.toolkit.fx.utils.ScrollPosition;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 *
 * @author Pavel Castornii
 */
public class CompletionPopupFxView<P extends CompletionPopupPresenter<?>>
        extends AbstractPopupFxView<P> implements CompletionPopupView {

    private final ListView<CompletionItem<?>> listView = new ListView<>();

    private final RichTextArea textArea = new RichTextArea();

    private final SplitPane splitPane = new SplitPane(listView, textArea);

    @Override
    public void requestFocus() {
        NodeUtils.requestFocus(listView);
    }

    @Override
    public void selectPrevious() {
        var current = this.listView.getSelectionModel().getSelectedIndex();
        if (current > 0) {
            var newIndex = current - 1;
            this.listView.getSelectionModel().select(newIndex);
            ListViewUtils.scrollToIfNeeded(listView, newIndex, ScrollPosition.CENTER);
        }
    }

    @Override
    public void selectNext() {
        var current = this.listView.getSelectionModel().getSelectedIndex();
        if (current + 1 < this.listView.getItems().size()) {
            var newIndex = current + 1;
            this.listView.getSelectionModel().select(newIndex);
            ListViewUtils.scrollToIfNeeded(listView, newIndex, ScrollPosition.END);
        }
    }

    @Override
    public void updateItems(List<CompletionItem<?>> items) {
        listView.setItems(FXCollections.observableArrayList(items));
        listView.getSelectionModel().select(0);
    }

    @Override
    public void displayInfo(String description, String module) {
        textArea.clear();
        var boldStyle = StyleAttributeMap.builder().setBold(true).build();
        textArea.appendText("Description: ", boldStyle);
        textArea.appendText(description + "\n\n", StyleAttributeMap.EMPTY);
        textArea.appendText("Module: ", boldStyle);
        textArea.appendText(module, StyleAttributeMap.EMPTY);
    }

    @Override
    public void displayInfo(String description, boolean required, String alias) {
        textArea.clear();
        var boldStyle = StyleAttributeMap.builder().setBold(true).build();
        textArea.appendText("Description: ", boldStyle);
        textArea.appendText(description + "\n\n", StyleAttributeMap.EMPTY);
        textArea.appendText("Required: ", boldStyle);
        textArea.appendText(String.valueOf(required) + "\n\n", StyleAttributeMap.EMPTY);
        textArea.appendText("Alias: ", boldStyle);
        textArea.appendText(alias, StyleAttributeMap.EMPTY);
    }

    @Override
    protected void build() {
        super.build();
        textArea.setPadding(new Insets(0, 5, 0, 5));
        textArea.setWrapText(true);
        textArea.getStyleClass().add("popup-text-area");
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getStyleClass().add("popup-split-pane");
        splitPane.setDividerPositions(0.35);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        getContentBox().getChildren().add(splitPane);
        getContentBox().getStyleClass().add("autocomplete-popup-box");

        getContentBox().setPrefSize(CompletionPopupConstants.WIDTH, CompletionPopupConstants.HEIGHT);
        getContentBox().setMinSize(CompletionPopupConstants.WIDTH, CompletionPopupConstants.HEIGHT);
        getContentBox().setMaxSize(CompletionPopupConstants.WIDTH, CompletionPopupConstants.HEIGHT);
        //note - stackpange doesn't include menu bar
//        StackPane.setMargin(this.getNode(), new Insets(viewModel.getTop(), 0, 0, viewModel.getLeft()));
        listView.setCellFactory(lv -> {
            ListCell<CompletionItem<?>> cell = new ListCell<>() {
                @Override
                protected void updateItem(CompletionItem<?> item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getText());
                }
            };
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getClickCount() == 2) {
                    getPresenter().onItemSubmitted(cell.getItem());
                }
            });
            return cell;
        });
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        this.listView.getSelectionModel().selectedItemProperty()
                .addListener((ov, oldV, newV) -> getPresenter().onItemSelected(newV));
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        getContentBox().addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                getPresenter().onClose();
            } else if (event.getCode() == KeyCode.ENTER) {
                getPresenter().onItemSubmitted(this.listView.getSelectionModel().getSelectedItem());
            }
        });
    }

    protected ListView<CompletionItem<?>> getListView() {
        return listView;
    }

    protected RichTextArea getTextArea() {
        return textArea;
    }

}
