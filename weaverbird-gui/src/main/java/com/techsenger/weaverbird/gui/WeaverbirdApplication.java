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

package com.techsenger.weaverbird.gui;

import com.techsenger.shellfx.core.CoreComponents;
import com.techsenger.shellfx.core.DefaultShellContext;
import com.techsenger.shellfx.core.DefaultShellFxView;
import com.techsenger.shellfx.core.DefaultShellParams;
import com.techsenger.shellfx.core.DefaultShellPresenter;
import com.techsenger.shellfx.core.area.AreaParams;
import com.techsenger.shellfx.core.history.InMemoryHistoryManager;
import com.techsenger.shellfx.core.registry.ControlRegistry;
import com.techsenger.shellfx.icons.Fonts;
import com.techsenger.shellfx.icons.IconStylesheetFactory;
import com.techsenger.shellfx.layout.tabhost.TabHostFxView;
import com.techsenger.shellfx.layout.tabhost.TabHostPresenter;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.menu.DefaultMenuGroupName;
import com.techsenger.shellfx.material.style.Density;
import com.techsenger.shellfx.material.style.IconStylesheets;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.shellfx.material.theme.AtlantaFxTheme;
import com.techsenger.toolkit.fx.color.ColorUtils;
import com.techsenger.weaverbird.gui.menu.FileMenuRegistrar;
import com.techsenger.weaverbird.gui.settings.ConsoleSettings;
import com.techsenger.weaverbird.gui.settings.LayoutEngine;
import com.techsenger.weaverbird.gui.settings.LineType;
import com.techsenger.weaverbird.gui.style.WeaverbirdIconStylesheets;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 *
 * @author Pavel Castornii
 */
public class WeaverbirdApplication extends Application {

    public static ConsoleSettings createSettings() {
        var settings = new ConsoleSettings();
        var appearance = settings.getAppearance();
        appearance.setRegularFont(Font.font("System", 14));
        appearance.setMonospaceFont(Font.font("Monospace", 14));
        appearance.setTheme(AtlantaFxTheme.CUPERTINO_DARK);
        appearance.setDensity(Density.S);
        var diagram = settings.getDiagram();
        diagram.setLayoutEngine(LayoutEngine.SMETANA);
        diagram.setLimitSize(10000);
        diagram.setLineType(LineType.ORTHO);
        diagram.setLayerColor(ColorUtils.toColor(appearance.getTheme().getPalette().getAccent3Color()));
        diagram.setModuleColor(ColorUtils.toColor(appearance.getTheme().getPalette().getDanger3Color()));
        return settings;
    }

    @Override
    public void start(Stage stage) throws Exception {
        FontIconView.setDefaultIconFont(Fonts.MATERIAL_DESIGN_ICONS.getFamily());
        IconStylesheets.addAll(IconStylesheetFactory.forAll());
        IconStylesheets.addAll(WeaverbirdIconStylesheets.getAll());

        var controlRegistry = new ControlRegistry(CoreComponents.SHELL, new DefaultMenuGroupName("MainMenuGroup"));
        var shellView = new DefaultShellFxView<>(this, stage, null, controlRegistry);
        var context = new DefaultShellContext(createSettings(), new InMemoryHistoryManager(), getHostServices());
        var shellParams = new DefaultShellParams(context);
        var shellPresenter = new DefaultShellPresenter<>(shellView, shellParams);
        shellPresenter.initialize();
        shellPresenter.setOnClosed(() -> Platform.exit());
        shellPresenter.setTitle("Weaverbird Framework");
        shellView.getStage().getScene().getRoot().getStyleClass().add(StyleClasses.DENSITY_S);

        var workspaceView = new TabHostFxView<>(true);
        var workspacePresenter = new TabHostPresenter<>(workspaceView, new AreaParams());
        workspacePresenter.initialize();
        shellView.getComposer().addWorkspace(workspaceView);

        var fileRegistrar = new FileMenuRegistrar(shellView, ModuleActivatorProvider.getFramework());
        fileRegistrar.register();

        shellView.upgradeMenuBar();
        stage.show();
    }

}
