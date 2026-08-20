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
import com.techsenger.shellfx.core.page.AbstractPageFxView;
import com.techsenger.shellfx.material.style.Spacing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 *
 * @author Pavel Castornii
 */
public class LayerPageFxView<P extends LayerPagePresenter<?>> extends AbstractPageFxView<P>
        implements LayerPageView {

    private static final Object SELECT_ALL_CHECK_BOX = new Object();

    /**
     * We use an adapter because we cannot obtain a stable {@link BooleanProperty} reference to mark the entire column
     * when the select all checkbox is clicked.
     */
    private final TableView<ModuleConfigAdapter> table = new TableView<>();

    private final VBox main = new VBox(table);

    private boolean listenersDisabled = false;

    public LayerPageFxView() {
        super();
    }

    @Override
    public void requestFocus() {

    }

    @Override
    public Region getNode() {
        return this.main;
    }

    @Override
    public void showLayer(LayerConfig layer) {
        buildTableRows(layer);
        //control box
        var controlBox = createControlBox(layer);
        this.main.getChildren().add(controlBox);
    }

    @Override
    public void refreshModules() {
        this.listenersDisabled = true;
        for (var m : this.table.getItems()) {
            m.update();
        }
        this.listenersDisabled = false;
    }

    @Override
    public void clearSelectAll() {
        this.listenersDisabled = true;
        for (var column : this.table.getColumns()) {
            var checkBox = getSelectAllCheckBox(column);
            if (checkBox != null) {
                checkBox.setSelected(false);
            }
        }
        this.listenersDisabled = false;
    }

    @Override
    protected void build() {
        super.build();
//        var includedLabel = new Label("Included");
//        var includedCheckBox = new CheckBox();
////        includedCheckBox.selectedProperty().bindBidirectional(viewModel.getLayerConfig().includedProperty());
//        var coloredLabel = new Label("Colored");
//        var coloredCheckBox = new CheckBox();
////        coloredCheckBox.selectedProperty().bindBidirectional(viewModel.getLayerConfig().coloredProperty());
////        titleBox.getChildren().addAll(getTitleLabel(), new Spacer(), includedLabel, includedCheckBox,
////                coloredLabel, coloredCheckBox);
////        titleBox.setSpacing(SizeConstants.INSET);
////        titleBox.setAlignment(Pos.CENTER_LEFT);
////        titleBox.setPadding(new Insets(0, 0, SizeConstants.INSET, 0));

        //table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setEditable(true);
        table.getStyleClass().add(Styles.DENSE);
        buildTableColumns();
        VBox.setVgrow(table, Priority.ALWAYS);

        main.setPadding(new Insets(0, Spacing.getHorizontal(), 0, Spacing.getHorizontal()));
        this.main.setSpacing(Spacing.getVerticalHalf());
        VBox.setVgrow(this.main, Priority.ALWAYS);
    }

    private void buildTableRows(LayerConfig layerComponent) {
        var modules = layerComponent.getModules().stream().map(m -> new ModuleConfigAdapter(m)).toList();
        table.setItems(FXCollections.observableArrayList(modules));
    }

    private void buildTableColumns() {
        //name column
        TableColumn<ModuleConfigAdapter, String> nameColumn = new TableColumn<>("Module");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        nameColumn.setEditable(false);
        //we disable column reodering because we use control pane under table
        nameColumn.setReorderable(false);
        nameColumn.setMinWidth(280);
        nameColumn.setStyle("-fx-alignment: center-left;");
        table.getColumns().addAll(
                nameColumn,
                createCheckBoxColumn("Included", ModuleConfigAdapter::includedProperty),
                createCheckBoxColumn("Reads", ModuleConfigAdapter::readsProperty),
                createCheckBoxColumn("Exports", ModuleConfigAdapter::exportsProperty),
                createCheckBoxColumn("Opens", ModuleConfigAdapter::opensProperty),
                createCheckBoxColumn("Requires", ModuleConfigAdapter::requiresProperty),
                createCheckBoxColumn("Requests", ModuleConfigAdapter::requestsProperty),
                createCheckBoxColumn("Colored", ModuleConfigAdapter::coloredProperty));
    }

    private TableColumn<ModuleConfigAdapter, Boolean> createCheckBoxColumn(String title,
            Function<ModuleConfigAdapter, BooleanProperty> f) {
        TableColumn<ModuleConfigAdapter, Boolean> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> {
            ModuleConfigAdapter module = data.getValue();
            var prop = f.apply(module);
            return prop;
        });
        column.setCellFactory(tc -> {
            return new CheckBoxTableCell<ModuleConfigAdapter, Boolean>();
        });
        column.setEditable(true);
        column.setReorderable(false);
        column.setStyle("-fx-alignment: center;");
        return column;
    }

    private HBox createControlBox(LayerConfig layer) {
        var nameBox = new HBox();
        this.bindWidths(nameBox, table.getColumns().get(0), -2);
        nameBox.getChildren().add(new Label("Select/Unselect All"));
        var mainBox = new HBox(nameBox);

        // initial values
        List<Boolean> initialValues = new ArrayList<>(Collections.nCopies(7, true));
        for (var m : this.table.getItems()) {
            getValueAndAddListener(m.includedProperty(), initialValues, 0);
            getValueAndAddListener(m.readsProperty(), initialValues, 1);
            getValueAndAddListener(m.exportsProperty(), initialValues, 2);
            getValueAndAddListener(m.opensProperty(), initialValues, 3);
            getValueAndAddListener(m.requiresProperty(), initialValues, 4);
            getValueAndAddListener(m.requestsProperty(), initialValues, 5);
            getValueAndAddListener(m.coloredProperty(), initialValues, 6);
        }
        // creating checkboxes
        mainBox.getChildren().addAll(
                createSelectAllCheckBox(ModuleConfigAdapter::includedProperty, 0, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::readsProperty, 1, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::exportsProperty, 2, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::opensProperty, 3, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::requiresProperty, 4, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::requestsProperty, 5, initialValues),
                createSelectAllCheckBox(ModuleConfigAdapter::coloredProperty, 6, initialValues));

        mainBox.setPadding(new Insets(Spacing.getVerticalHalf(), 0, 0, 0));
        return mainBox;
    }

    private HBox createSelectAllCheckBox(Function<ModuleConfigAdapter, BooleanProperty> f,
            int index, List<Boolean> initialValues) {
        var column = this.table.getColumns().get(index + 1);
        var checkBox = new CheckBox();
        column.getProperties().put(SELECT_ALL_CHECK_BOX, checkBox);
        checkBox.setSelected(initialValues.get(index));
        checkBox.selectedProperty().addListener((ov, oldV, newV) -> onSelectAllValueChanged(newV, f));
        var hBox = new HBox(checkBox);
        hBox.setAlignment(Pos.CENTER);
        hBox.getStyleClass().add("check-box-table-cell");
        this.bindWidths(hBox, column, 0);
        return hBox;
    }

    private void getValueAndAddListener(BooleanProperty prop, List<Boolean> initValues, int index) {
        if (!prop.get()) {
            initValues.set(index, false);
        }
        prop.addListener((ov, oldV, newV) -> onColumnValueChanged(table.getColumns().get(index + 1)));
    }

    private void onColumnValueChanged(TableColumn<ModuleConfigAdapter, ?> column) {
        if (listenersDisabled) {
            return;
        }
        this.listenersDisabled = true;
        var checkBox = getSelectAllCheckBox(column);
        if (checkBox == null) {
            this.listenersDisabled = false;
            return;
        }
        boolean selectedAll = true;
        for (int i = 0; i < this.table.getItems().size(); i++) {
            Boolean val = (Boolean) column.getCellData(i);
            if (Boolean.FALSE.equals(val)) {
                selectedAll = false;
                break;
            }
        }
        checkBox.setSelected(selectedAll);
        this.listenersDisabled = false;
    }

    private void onSelectAllValueChanged(boolean newV, Function<ModuleConfigAdapter, BooleanProperty> f) {
        if (this.listenersDisabled) {
            return;
        }
        this.listenersDisabled = true;
        for (var m : this.table.getItems()) {
            BooleanProperty prop = f.apply(m);
            prop.set(newV);
        }
        this.listenersDisabled = false;
    }

    private void bindWidths(HBox box, TableColumn<?, ?> column, int correction) {
        box.prefWidthProperty().bind(Bindings.add(column.widthProperty(), correction));
    }

    private CheckBox getSelectAllCheckBox(TableColumn<ModuleConfigAdapter, ?> column) {
        CheckBox checkBox = (CheckBox) column.getProperties().get(SELECT_ALL_CHECK_BOX);
        return checkBox;
    }
}
