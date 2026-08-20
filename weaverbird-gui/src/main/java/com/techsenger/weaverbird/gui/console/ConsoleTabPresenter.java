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
import com.techsenger.shellfx.core.UiExecutor;
import com.techsenger.shellfx.core.settings.SettingsSubscription;
import com.techsenger.shellfx.core.tab.AbstractHostTabPresenter;
import com.techsenger.weaverbird.core.api.Constants;
import com.techsenger.weaverbird.core.api.message.DefaultMessage;
import com.techsenger.weaverbird.core.api.message.Message;
import com.techsenger.weaverbird.core.api.message.MessageType;
import com.techsenger.weaverbird.executor.api.CommandExecutor;
import com.techsenger.weaverbird.executor.api.CommandExecutorFactory;
import com.techsenger.weaverbird.executor.api.CommandSyntax;
import com.techsenger.weaverbird.executor.api.command.Commands;
import com.techsenger.weaverbird.gui.WeaverbirdComponents;
import com.techsenger.weaverbird.gui.style.WeaverbirdIcons;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pavel Castornii
 */
public class ConsoleTabPresenter<V extends ConsoleTabView> extends AbstractHostTabPresenter<V>
        implements CompletionPopupAwarePort, ConsoleToolBarAwarePort {

    /**
     * Input without prompt. Contains the information about the current token (next to caret).
     *
     * @param text the text without prompt
     * @param caretOffset the caret offset inside input text.
     */
    private record PromptlessInput(String text, int caretOffset, int elementIndex,
        int elementLength, boolean elementFirst) { };

    private static final Logger logger = LoggerFactory.getLogger(ConsoleTabPresenter.class);

    private static final String PROMPT = "> ";

    /**
     * Returns the start index and length of the word near the caret position.
     * The caret can be inside the word or directly before/after it without a space.
     *
     * @param text   the text to search in
     * @param offset the caret position
     * @return int[]{start, length} or null if no word is found near the caret
     */
    public static int[] getTokenRange(String text, int offset) {
        if (text == null || text.isEmpty() || offset < 0 || offset > text.length()) {
            return null;
        }
        // Check character at caret position, then fallback to the left
        int pos = offset;
        if (pos == text.length() || !isTokenChar(text.charAt(pos))) {
            pos = pos - 1;
        }
        // No word character found near caret
        if (pos < 0 || !isTokenChar(text.charAt(pos))) {
            return null;
        }
        // Walk left to find the start of the word
        while (pos > 0 && isTokenChar(text.charAt(pos - 1))) {
            pos--;
        }
        int start = pos;
        // Walk right to find the end of the word
        while (pos < text.length() && isTokenChar(text.charAt(pos))) {
            pos++;
        }
        int length = pos - start;
        return new int[]{start, length};
    }

    private static boolean isTokenChar(char ch) {
        return Character.isLetterOrDigit(ch)
                || ch == '-'
                || String.valueOf(ch).equals(Constants.NAME_VERSION_SEPARATOR)
                || String.valueOf(ch).equals(CommandSyntax.LOCAL_COMMAND);
    }

    private volatile String sessionPrompt;

    /**
     * Command executor that will execute commands from this console.
     */
    private final CommandExecutor executor;

    private SettingsSubscription fontSubscription;

    private Font monospaceFont;

    /**
     * If null then the caret is outside of the editable region.
     */
    private Integer caretOffset;

    private PromptlessInput input;

    /**
     * The history of the commands.
     */
    private final List<String> lastCommands = new ArrayList<>();

    /**
     * Keeps the index of the last history scrolling command. It is reset after every command.
     */
    private volatile int commandIndex = -1;

    public ConsoleTabPresenter(V view, ConsoleTabParams params) {
        super(view, params);
        var composer = getView().getComposer();
        composer.setClient(params.getClient());
        composer.setSession(params.getSession());
        CommandExecutor ex = null;
        try {
            ex = CommandExecutorFactory.create(params.getFramework(), params.getClient());
        } catch (Exception e) {
            logger.error("{} Error creating executor", getDescriptor().getLogPrefix(), e);
        }
        this.executor = ex;
    }

    @Override
    protected ComponentDescriptor createDescriptor() {
        return new ComponentDescriptor(WeaverbirdComponents.CONSOLE_TAB);
    }

    @Override
    public CloseCheckResult isReadyToClose() {
        return CloseCheckResult.READY;
    }

    @Override
    public void prepareToClose(Consumer<ClosePreparationResult> resultCallback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSessionPrompt() {
        return sessionPrompt;
    }

    public Font getMonospaceFont() {
        return this.monospaceFont;
    }

    @Override
    public void onElementSubmitted(CompletionType type, String text) {
        addElement(type, text);
    }

    @Override
    public void onPopupClose() {
        getView().getComposer().closePopup();
        getView().requestFocus();
    }

    @Override
    public void onClear() {
        this.lastCommands.clear();
        this.commandIndex = -1;
        getView().clear();
        getView().printPrompt(getPrompt());
        getView().requestFocus();
    }

    @Override
    public void onCopy() {
        getView().copy();
    }

    @Override
    public void onPaste() {
        getView().paste();
    }

    @Override
    public void onSessionChanged(ClientSession session) {
        var cmdContext = this.executor.getCommandContext();
        if (cmdContext.getSession() != session) {
            cmdContext.setSession(session);
            updatePrompt();
            getView().updatePrompt(getPrompt());
            getView().requestFocus();
        }
    }

    @Override
    public void onSelected(boolean selected) {
        super.onSelected(selected);
        if (selected) {
            getView().requestFocus();
        }
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        setTitle("Console");
        setIcon(WeaverbirdIcons.CONSOLE);
        updatePrompt();
        showPrompt();
        getView().requestFocus();
        var settings = getView().getComposer().getShellPort().getContext().getSettings().getAppearance();
        setMonospaceFont(settings.getMonospaceFont());
        this.fontSubscription = settings.onMonospaceFontChanged((oldV, newV) -> setMonospaceFont(newV));
        getView().highlightCommands(executor.getCommandsByName().keySet());
    }

    @Override
    protected void preDeinitialize() {
        super.preDeinitialize();
        this.fontSubscription.unsubscribe();
    }

    protected void showPrompt() {
        UiExecutor.execute(() -> getView().printPrompt(getPrompt()));
    }

    protected void onElementSubmitted() {
        var composer = getView().getComposer();
        var popup = composer.getPopupPort();
        addElement(popup.getType(), popup.getSelectedItemText());
        composer.closePopup();
        getView().requestFocus();
    }

    protected void onAutocomplete(String paragraph) {
        this.input = createInput(paragraph);
        String elementToken = null;
        int offset = this.input.caretOffset + getPrompt().length();
        if (this.input.elementIndex >= 0) {
            elementToken = input.text.substring(input.elementIndex, input.elementIndex + input.elementLength);
            offset = this.input.elementIndex + getPrompt().length();
        }

        var processingCommand = false;
        if ((input.text.substring(0, input.caretOffset).isBlank() || input.elementFirst)) {
            processingCommand = true;
        }

        if (processingCommand) {
            var commands = executor.getCommandsByName().values();
            var sessionExists = executor.getCommandContext().getSession() != null;
            getView().getComposer().openCommandPopup(commands, sessionExists, elementToken, offset);
        } else {
            var splits = this.input.text.trim().split(Pattern.quote(" "));
            var cmd = splits[0].trim();
            if (cmd.startsWith(CommandSyntax.LOCAL_COMMAND) && cmd.length() > 1) {
                cmd = cmd.substring(1);
            }
            var command = executor.getCommandsByName().get(cmd);
            if (command != null) {
                getView().getComposer().openParameterPopup(command.getParameters(), elementToken, offset);
            }
        }
    }

    protected void onCaretChanged(Integer caretOffset) {
        this.caretOffset = caretOffset;
    }

    protected void onCopyAvailable(boolean value) {
        getView().getComposer().getToolBarPort().onCopyAvailable(value);
    }

    protected void onCommandsSubmitted(String paragraph, int width) {
        var input = paragraph.substring(getPrompt().length()); // removing prompt
        if (input.isEmpty()) {
            this.showPrompt();
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                lastCommands.add(input); // even if the command can fail
                commandIndex = -1;
                var oldSession = executor.getCommandContext().getSession();
                var results = executor.executeCommands(input, null, null, width);
                results.forEach(r -> {
                if (!r.getCommandName().equals(Commands.LOG_PRINT)) {
                    UiExecutor.execute(() -> getView().printMessages(r.getMessages()));
                }
//                else {
//                    this.logMessages.next(r.getMessages());
//                }
                });
                var newSession = executor.getCommandContext().getSession();
                if (!Objects.equals(oldSession, newSession)) {
                    UiExecutor.execute(() -> getView().getComposer().getToolBarPort().updateSession(newSession));
                }
                updatePrompt();
                // syncSessionBarAndContext(null);
                this.showPrompt();
            } catch (Exception ex) {
                logger.error("{} Error executing commands from GUI console", getDescriptor().getLogPrefix(), ex);
                List<Message> messages = new ArrayList<>();
                messages.add(new DefaultMessage(MessageType.ERROR, ex.getMessage()));
                if (ex.getCause() != null) {
                    messages.add(new DefaultMessage(MessageType.ERROR, ex.getCause().getMessage()));
                }
                messages.add(new DefaultMessage(MessageType.OUTPUT,
                        "Enter \"command:list\" to get a list of all commands."));
                messages.add(new DefaultMessage(MessageType.OUTPUT,
                        "Enter \"command -?\" to get help on a specific command."));
                UiExecutor.execute(() -> getView().printMessages(messages));
                this.showPrompt();
            }
        });
    }

    protected void onTextInput(String paragraph) {
        var popup = getView().getComposer().getPopupPort();
        if (popup == null) {
            return;
        }
        var input = createInput(paragraph);
        if (popup.getType() == CompletionType.COMMAND) {
            if (input.elementIndex >= 0) {
                var command = input.text.substring(input.elementIndex, input.elementIndex + input.elementLength);
                popup.updateItems(command);
            }
        }
    }

    protected void onMoveUp() {
        var popup = getView().getComposer().getPopupPort();
        if (popup == null) {
            scrollHistoryUp();
        } else {
            popup.moveUp();
        }
    }

    protected void onMoveDown() {
        var popup = getView().getComposer().getPopupPort();
        if (popup == null) {
            scrollHistoryDown();
        } else {
            popup.moveDown();
        }
    }

    protected void setSessionPrompt(String sessionPrompt) {
        this.sessionPrompt = sessionPrompt;
    }

    protected void setMonospaceFont(Font font) {
        if (Objects.equals(this.monospaceFont, font)) {
            return;
        }
        this.monospaceFont = font;
        getView().updateMonospaceFont(font);
    }

    protected String buildSessionPrompt(ClientSession session) {
        var sb = new StringBuilder();
        sb.append(session.getLoginName());
        sb.append("@");
        sb.append(session.getHost());
        sb.append(PROMPT);
        return sb.toString();
    }

    protected void scrollHistoryUp() {
        int index = 0;
        if (this.commandIndex == -1) {
            index = this.lastCommands.size() - 1;
        } else {
            index = this.commandIndex - 1;
        }
        if (index >= 0) {
            this.commandIndex = index;
            getView().replaceInput(lastCommands.get(index));
        } else {
            getView().beep();
        }
    }

    protected void scrollHistoryDown() {
        int index = 0;
        if (this.commandIndex == -1) {
            index = -1;
        } else {
            index = this.commandIndex + 1;
        }
        if (index >= 0 && index < this.lastCommands.size()) {
            this.commandIndex = index;
            getView().replaceInput(lastCommands.get(index));
        } else if (index == this.lastCommands.size()) {
            this.commandIndex = -1;
            getView().replaceInput("");
        } else {
            getView().beep();
        }
    }

    private void addElement(CompletionType type, String element) {
        String oldInput = null;
        if (this.input.elementIndex >= 0) {
            oldInput = this.input.text.substring(0, this.input.elementIndex);
        } else {
            oldInput = this.input.text.substring(0, this.input.caretOffset);
        }
        var newInput = oldInput + (element == null ? "" : element + " ");
        getView().replaceInput(newInput);
        getView().getComposer().closePopup();
        getView().requestFocus();
        this.input = null;
    }

    private PromptlessInput createInput(String paragraph) {
        var input = paragraph.substring(getPrompt().length()); // removing prompt
        var inputOffset = caretOffset - getPrompt().length();
        int elemIndex = -1;
        int elemLength = -1;
        boolean elemFirst = false;
        var wordRange = getTokenRange(input, inputOffset);
        if (wordRange != null) {
            elemIndex = wordRange[0];
            elemLength = wordRange[1];
            elemFirst = input.substring(0, elemIndex).isBlank();
        }
        return new PromptlessInput(input, inputOffset, elemIndex, elemLength, elemFirst);
    }

    private void updatePrompt() {
        var session = executor.getCommandContext().getSession();
        if (session != null) {
            this.sessionPrompt = buildSessionPrompt(session);
        } else {
            this.sessionPrompt = null;
        }
    }

    private String getPrompt() {
        if (this.sessionPrompt != null) {
            return this.sessionPrompt;
        } else {
            return PROMPT;
        }
    }
}
