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

package com.techsenger.weaverbird.gui.menu;

import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.registry.AbstractControlRegistrar;
import com.techsenger.shellfx.core.registry.ControlFactory;
import com.techsenger.shellfx.layout.tabhost.TabHostFxView;
import com.techsenger.shellfx.material.icon.FontIconView;
import com.techsenger.shellfx.material.menu.AbstractMenuItemHandler;
import com.techsenger.shellfx.material.menu.ManagedMenu;
import com.techsenger.shellfx.material.menu.ManagedMenuGroup;
import com.techsenger.shellfx.material.menu.ManagedMenuItem;
import com.techsenger.shellfx.material.menu.MenuItemHandler;
import com.techsenger.weaverbird.core.api.Framework;
import com.techsenger.weaverbird.gui.console.ConsoleTabFxView;
import com.techsenger.weaverbird.gui.console.ConsoleTabParams;
import com.techsenger.weaverbird.gui.console.ConsoleTabPresenter;
import com.techsenger.weaverbird.gui.diagram.DiagramTabFxView;
import com.techsenger.weaverbird.gui.diagram.DiagramTabParams;
import com.techsenger.weaverbird.gui.diagram.DiagramTabPresenter;
import com.techsenger.weaverbird.gui.settings.ConsoleSettings;
import com.techsenger.weaverbird.gui.style.WeaverbirdIcons;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientServiceFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 *
 * @author Pavel Castornii
 */
public class FileMenuRegistrar extends AbstractControlRegistrar {

    private final ShellFxView<?> shell;

    private final Framework framework;

    private final ClientService client = ClientServiceFactory.create();

    public FileMenuRegistrar(ShellFxView<?> shell, Framework framework) {
        super(shell.getControlRegistry());
        this.shell = shell;
        this.framework = framework;
    }

    @Override
    public void register() {
        registerMenu();
        registerMainGroup();
        registerConsoleItem();
        registerDiagramItem();
//        registerSettingsItem();
    }

    protected void registerMenu() {
        ControlFactory<ShellFxView<?>, ManagedMenu> f = (v) -> {
            return new ManagedMenu(FileMenu.NAME, "_File", 0);
        };
        addRegistration(getRegistry().mainMenu().registerMenu(f));
    }

    private void registerMainGroup() {
        ControlFactory<ShellFxView<?>, ManagedMenuGroup> f = (v) -> new ManagedMenuGroup(FileMenu.MAIN, 100);
        addRegistration(getRegistry().mainMenu().registerMenuGroup(FileMenu.NAME, f));
    }

    private void registerConsoleItem() {
        ControlFactory<ShellFxView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem("C_onsole", 100);
            item.setGraphic(new FontIconView(WeaverbirdIcons.CONSOLE));
            item.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
            var handler = new AbstractMenuItemHandler<ShellFxView<?>, ManagedMenuItem>(shell, item) {

                @Override
                public void onAction() {
                    var shell = getComponent();
                    var consoleView = new ConsoleTabFxView<>(shell);
                    var params = new ConsoleTabParams(framework, client, null);
                    var consolePresenter = new ConsoleTabPresenter<>(consoleView, params);
                    consolePresenter.initialize();
                    TabHostFxView<?> workspace = (TabHostFxView<?>) shell.getComposer().getWorkspace();
                    workspace.getComposer().addTab(consoleView);
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;
        };
        addRegistration(getRegistry().mainMenu().registerMenuItem(FileMenu.MAIN, f));
    }

    private void registerDiagramItem() {
        ControlFactory<ShellFxView<?>, ManagedMenuItem> f = (v) -> {
            var item = new ManagedMenuItem("D_iagrams", 200);
            item.setGraphic(new FontIconView(WeaverbirdIcons.DIAGRAMS));
            item.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
            var handler = new AbstractMenuItemHandler<ShellFxView<?>, ManagedMenuItem>(shell, item) {
                @Override
                public void onAction() {
                    var shell = getComponent();
                    var diagramView = new DiagramTabFxView<>(shell);
                    ConsoleSettings consoleSettings = (ConsoleSettings) shell.getPresenter().getContext().getSettings();
                    var params = new DiagramTabParams(framework, client, null, consoleSettings.getDiagram());
                    var diagramPresenter = new DiagramTabPresenter<>(diagramView, params);
                    diagramPresenter.initialize();
                    TabHostFxView<?> workspace = (TabHostFxView<?>) shell.getComposer().getWorkspace();
                    workspace.getComposer().addTab(diagramView);
                }
            };
            MenuItemHandler.setHandler(item, handler);
            return item;

        };
        addRegistration(getRegistry().mainMenu().registerMenuItem(FileMenu.MAIN, f));
    }

//    private void registerSettingsItem() {
//        ControlFactory<KeyedMenuItem> f = (v) -> {
//            var tabShellView = (TabShellView<?>) v;
//            var item = new KeyedMenuItem(FileMenuKeys.SETTINGS, false, false, false, "_Settings",
//                    new FontIconView(WeaverbirdIcons.SETTINGS));
//            item.setOnAction((e) -> {
//                var viewModel =
//                        new SettingsDialogViewModel((ConsoleSettings) tabShellView.getViewModel().getSettings(),
//                        tabShellView.getViewModel().getHistoryManager());
//                var view = new SettingsDialogView(viewModel);
//                view.initialize();
//                tabShellView.getDialogManager().openDialog(view);
//            });
//            return item;
//
//        };
//        addRegistration(getRegistry().registerMenuItem(TabShellKey.INSTANCE, FileMenuKeys.MAIN, f, 1000));
//    }
}
