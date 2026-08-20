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

import com.techsenger.patternfx.mvp.ComponentDescriptor;
import com.techsenger.shellfx.core.CloseCheckResult;
import com.techsenger.shellfx.core.ClosePreparationResult;
import com.techsenger.shellfx.core.popup.AbstractPopupPresenter;
import com.techsenger.weaverbird.executor.api.CommandSyntax;
import com.techsenger.weaverbird.executor.api.command.CommandInfo;
import com.techsenger.weaverbird.executor.api.command.ParameterDescriptor;
import com.techsenger.weaverbird.gui.WeaverbirdComponents;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Pavel Castornii
 */
public class CompletionPopupPresenter<V extends CompletionPopupView> extends AbstractPopupPresenter<V>
        implements CompletionPopupPort {

    private final Collection<CommandInfo> commands;

    private final List<ParameterDescriptor> parameters;

    private final CompletionType type;

    private final CompletionPopupAwarePort popupAware;

    private List<CompletionItem<?>> items;

    private CompletionItem<?> selectedItem;

    private String token;

    private final boolean sessionExists;

    public CompletionPopupPresenter(V view, CompletionPopupParams params) {
        super(view, params);
        this.commands = params.getCommands();
        this.parameters = params.getParameterDescriptors();
        if (commands != null) {
            this.type = CompletionType.COMMAND;
        } else {
            this.type = CompletionType.PARAMETER;
        }
        this.popupAware = params.getPopupAware();
        this.token = params.getToken();
        this.sessionExists = params.isSessionExists();
    }

    @Override
    protected ComponentDescriptor createDescriptor() {
        return new ComponentDescriptor(WeaverbirdComponents.COMPLETION_POPUP);
    }

    @Override
    public CloseCheckResult isReadyToClose() {
        return CloseCheckResult.READY;
    }

    @Override
    public void prepareToClose(Consumer<ClosePreparationResult> resultCallback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CompletionType getType() {
        return type;
    }

    public List<CompletionItem<?>> getItems() {
        return this.items;
    }

    @Override
    public String getSelectedItemText() {
        return getItemText(selectedItem);
    }

    @Override
    public void updateItems(String text) {
        if (this.type == CompletionType.COMMAND) {
            setItems(createCommandItems(text));
        } else {
            setItems(createParameterItems(text));
        }
    }

    @Override
    public void moveUp() {
        getView().selectPrevious();
    }

    @Override
    public void moveDown() {
        getView().selectNext();
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        if (this.type == CompletionType.COMMAND) {
            setItems(createCommandItems(this.token));
        } else {
            setItems(createParameterItems(token));
        }
    }

    protected void onItemSelected(CompletionItem<?> item) {
        this.selectedItem = item;
        if (this.selectedItem != null) {
            if (this.type == CompletionType.COMMAND) {
                var command = ((CompletionItem<CommandInfo>) item).getElement();
                getView().displayInfo(command.getDescription(), command.getModuleName());
            } else {
                var parameter = ((CompletionItem<ParameterDescriptor>) item).getElement();
                getView().displayInfo(parameter.getDescription(), parameter.isRequired(), parameter.getShortName());
            }
        }
    }

    protected void onItemSubmitted(CompletionItem<?> item) {
        this.popupAware.onElementSubmitted(type, getItemText(item));
    }

    protected void onClose() {
        this.popupAware.onPopupClose();
    }

    protected void setItems(List<CompletionItem<?>> items) {
        this.items = items;
        getView().updateItems(items);
    }

    private List<CompletionItem<?>> createCommandItems(String token) {
        var local = !sessionExists;
        var prefix = "";
        if (token != null && token.trim().startsWith(CommandSyntax.LOCAL_COMMAND)) {
            local = true;
            prefix = CommandSyntax.LOCAL_COMMAND;
        }
        var finalLocal = local;
        var finalPrefix = prefix;
        return this.commands.stream()
                .filter(c -> finalLocal ? c.isLocal() : c.isRemote())
                .<CompletionItem<?>>map(c -> new CompletionItem<>(finalPrefix + c.getName(), c))
                .filter(c -> token == null ? true : c.getText().startsWith(token))
                .sorted(Comparator.comparing(CompletionItem::getText))
                .toList();
    }

    private List<CompletionItem<?>> createParameterItems(String token) {
        return this.parameters.stream()
                .filter(c -> token == null ? true : c.getLongName().startsWith(token))
                .<CompletionItem<?>>map(c ->
                        new CompletionItem<>(c.isRequired() ? c.getLongName() + "*" : c.getLongName(), c))
                .sorted(Comparator
                        .<CompletionItem<?>, Boolean>comparing(i -> !i.getText().endsWith("*"))
                        .thenComparing(i -> i.getText()))
                .toList();
    }

    private String getItemText(CompletionItem<?> item) {
        if (this.type == CompletionType.COMMAND) {
            return item.getText();
        } else {
            ParameterDescriptor param = (ParameterDescriptor) item.getElement();
            return param.isMain() ? null : param.getLongName();
        }
    }
}
