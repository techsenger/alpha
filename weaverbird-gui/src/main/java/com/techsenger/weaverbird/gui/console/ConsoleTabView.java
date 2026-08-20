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

import com.techsenger.shellfx.core.tab.HostTabView;
import com.techsenger.weaverbird.core.api.message.Message;
import com.techsenger.weaverbird.executor.api.command.CommandInfo;
import com.techsenger.weaverbird.executor.api.command.ParameterDescriptor;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javafx.scene.text.Font;

/**
 *
 * @author Pavel Castornii
 */
public interface ConsoleTabView extends HostTabView {

    interface Composer extends HostTabView.Composer {

        void setClient(ClientService client);

        void setSession(ClientSession session);

        ConsoleToolBarPort getToolBarPort();

        void openCommandPopup(Collection<CommandInfo> commands, boolean sessionExists, String token, int offset);

        void openParameterPopup(List<ParameterDescriptor> parameters,  String token, int offset);

        CompletionPopupPort getPopupPort();

        void closePopup();
    }

    @Override
    Composer getComposer();

    void updateMonospaceFont(Font font);

    void printPrompt(String prompt);

    void updatePrompt(String prompt);

    void printMessages(List<Message> messages);

    void highlightCommands(Set<String> commands);

    void replaceInput(String text);

    void beep();

    void clear();

    void copy();

    void paste();
}
