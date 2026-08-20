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

import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.tab.AbstractHostTabFxView;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.weaverbird.core.api.model.ComponentLayerModel;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author Pavel Castornii
 */
public class DiagramTabFxView<P extends DiagramTabPresenter<?>> extends AbstractHostTabFxView<P>
        implements DiagramTabView {

    protected class Composer extends AbstractHostTabFxView<P>.Composer implements DiagramTabView.Composer {

        private final DiagramTabFxView<P> view = DiagramTabFxView.this;

        private ClientService client;

        private ClientSession session;

        @Override
        public void setClient(ClientService client) {
            this.client = client;
        }

        @Override
        public void setSession(ClientSession session) {
            this.session = session;
        }

        @Override
        public LayerDialogPort openLayerDialog(List<ComponentLayerModel> layerModels,
                List<LayerConfig> previousLayerConfigs) {
            var dialog = createDialog(layerModels, previousLayerConfigs);
            addDialog(dialog);
            return dialog.getPresenter();
        }

        @Override
        public void compose() {
            super.compose();
            var toolBar = createToolBar();
            getModifiableChildren().add(toolBar);
            view.getContentBox().getChildren().add(0, toolBar.getNode());
        }

        protected DiagramToolBarFxView<?> createToolBar() {
            var view = new DiagramToolBarFxView<>();
            var params = new DiagramToolBarParams(client, session, getPresenter());
            var presenter = new DiagramToolBarPresenter<>(view, params);
            presenter.initialize();
            return view;
        }

        protected LayerDialogFxView<?> createDialog(List<ComponentLayerModel> layerModels,
                List<LayerConfig> previousLayerConfigs) {
            var view = new LayerDialogFxView<>();
            var params = new LayerDialogParams(getShellPort().getContext().getSettings().getAppearance(),
                    layerModels, previousLayerConfigs);
            var presenter = new LayerDialogPresenter<>(view, params);
            presenter.initialize();
            return view;
        }
    }

    private final ImageView imageView = new ImageView();

    private final StackPane imageContainer = new StackPane(imageView);

    private final ScrollPane scrollPane = new ScrollPane(imageContainer);

    public DiagramTabFxView(ShellFxView<?> shell) {
        super(shell);
//        if (viewModel.getSessions() != null) {
//            sessions = new SessionsToolBarView(viewModel.getSessions());
//            sessions.initialize();
//        } else {
//            sessions = null;
//        }
    }

    @Override
    public void requestFocus() {
        imageView.requestFocus();
    }

    @Override
    public Composer getComposer() {
        return (Composer) super.getComposer();
    }

    @Override
    public void updateDiagram(Image diagram) {
        imageView.setImage(diagram);
    }

    @Override
    public void updateDiagramSize(double width, double height) {
        this.imageView.setFitWidth(width);
        this.imageView.setFitHeight(height);
    }

    @Override
    protected Composer createComposer() {
        return new DiagramTabFxView.Composer();
    }

    @Override
    protected void build() {
        super.build();
        imageView.setPickOnBounds(true); //to get scroll events even on transparent image section
        imageView.setPreserveRatio(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.getStyleClass().add(StyleClasses.NO_BORDER);


//        if (sessions != null) {
//            toolBars.getChildren().add(sessions.getNode());
//        }
        getContentBox().getChildren().add(scrollPane);
    }

    @Override
    protected void bind() {
        super.bind();
        imageContainer.minWidthProperty().bind(scrollPane.viewportBoundsProperty().map(b -> b.getWidth()));
        imageContainer.minHeightProperty().bind(scrollPane.viewportBoundsProperty().map(b -> b.getHeight()));
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
//        layerDiagramButton.setOnAction((e) -> viewModel.openLayerDialog());
//        scrollPane.setOnScroll((ScrollEvent event) -> {
//            if (event.isControlDown()) {
//                //there can be several events non zero and zero deltas
//                if (event.getDeltaY() < 0) {
//                    viewModel.zoomOut();
//                } else if (event.getDeltaY() > 0) {
//                    viewModel.zoomIn();
//                }
//                event.consume();
//            }
//        });
//        this.zoomInButton.setOnAction(e -> viewModel.zoomIn());
//        this.zoomOutButton.setOnAction(e -> viewModel.zoomOut());
    }
//
//    @Override
//    protected ComponentHelper<?> createComponentHelper() {
//        return new DiagramTabHelper(this);
//    }
}
