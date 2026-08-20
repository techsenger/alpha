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

import com.techsenger.shellfx.core.ShellFxView;
import com.techsenger.shellfx.core.tab.AbstractHostTabFxView;
import com.techsenger.shellfx.material.Anchors;
import com.techsenger.shellfx.material.style.StyleClasses;
import com.techsenger.shellfx.material.style.StyleUtils;
import com.techsenger.shellfx.material.theme.AtlantaFxTheme;
import com.techsenger.toolkit.fx.utils.NodeUtils;
import com.techsenger.weaverbird.core.api.message.Message;
import com.techsenger.weaverbird.core.api.message.MessageType;
import com.techsenger.weaverbird.executor.api.command.CommandInfo;
import com.techsenger.weaverbird.executor.api.command.ParameterDescriptor;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.StyleHandlerRegistry;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 *
 * @author Pavel Castornii
 */
public class ConsoleTabFxView<P extends ConsoleTabPresenter<?>> extends AbstractHostTabFxView<P>
        implements ConsoleTabView {

    private static final class CssRichTextArea extends RichTextArea {

        private static final StyleHandlerRegistry registry = createRegistry();

        private static StyleHandlerRegistry createRegistry() {
            // brings in the handlers from the base class
            StyleHandlerRegistry.Builder b = StyleHandlerRegistry.builder(RichTextArea.styleHandlerRegistry);
            // adds a handler for the new attribute
            b.setSegHandler(TextAreaCssStyles.CSS_ATTRIBUTE, (c, cx, style) -> {
                cx.addStyle(style);
            });
            return b.build();
        }

        @Override
        public StyleHandlerRegistry getStyleHandlerRegistry() {
            return registry;
        }
    };

    protected class Composer extends AbstractHostTabFxView<P>.Composer implements ConsoleTabView.Composer {

        private final ConsoleTabFxView<P> view = ConsoleTabFxView.this;

        private ConsoleToolBarFxView<?> toolBar;

        private CompletionPopupFxView<?> completionPopup;

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
        public ConsoleToolBarPort getToolBarPort() {
            return this.toolBar == null ? null : this.toolBar.getPresenter();
        }

        @Override
        public void compose() {
            super.compose();
            this.toolBar = createToolBar(client, session);
            getModifiableChildren().add(this.toolBar);
            view.getContentBox().getChildren().add(0, toolBar.getNode());
        }

        @Override
        public void openCommandPopup(Collection<CommandInfo> commands, boolean sessionExists, String token,
                int offset) {
            this.completionPopup = createCommandPopup(commands, sessionExists, token);
            var topLeft = calculatePopupPosition(offset);
            addPopup(this.completionPopup, Anchors.topLeft(topLeft.getY(), topLeft.getX()));
        }

        @Override
        public void openParameterPopup(List<ParameterDescriptor> parameters, String token, int offset) {
            this.completionPopup = createParameterPopup(parameters, token);
            var topLeft = calculatePopupPosition(offset);
            addPopup(this.completionPopup, Anchors.topLeft(topLeft.getY(), topLeft.getX()));
        }

        @Override
        public void closePopup() {
            if (this.completionPopup == null) {
                return;
            }
            closePopup(completionPopup);
            this.completionPopup = null;
            view.textArea.requestFocus();
        }

        @Override
        public CompletionPopupPort getPopupPort() {
            return this.completionPopup == null ? null : this.completionPopup.getPresenter();
        }

        protected ConsoleToolBarFxView<?> createToolBar(ClientService client, ClientSession session) {
            var view = new ConsoleToolBarFxView<>();
            var params = new ConsoleToolBarParams(client, session, getPresenter());
            var presenter = new ConsoleToolBarPresenter<>(view, params);
            presenter.initialize();
            return view;
        }

        protected CompletionPopupFxView<?> createCommandPopup(Collection<CommandInfo> commands,
                boolean sessionExists, String token) {
            var view = new CompletionPopupFxView<>();
            var params = new CompletionPopupParams(commands, sessionExists, null, token,
                    getPresenter());
            var presenter = new CompletionPopupPresenter<>(view, params);
            presenter.initialize();
            return view;
        }

        protected CompletionPopupFxView<?> createParameterPopup(List<ParameterDescriptor> parameterDescriptors,
                String token) {
            var view = new CompletionPopupFxView<>();
            var params = new CompletionPopupParams(null, false, parameterDescriptors, token,
                    getPresenter());
            var presenter = new CompletionPopupPresenter<>(view, params);
            presenter.initialize();
            return view;
        }

        private Point2D calculatePopupPosition(int offset) {
            Node caret = getCaret();

            Bounds boundsInScreen = caret.localToScreen(caret.getBoundsInLocal());
            if (boundsInScreen == null) {
                return new Point2D(0, 0);
            }

            Bounds boundsInStackPane = getWrapperPane().screenToLocal(boundsInScreen);
            if (boundsInStackPane == null) {
                return new Point2D(0, 0);
            }

            // double caretX = boundsInStackPane.getMinX() - (offset * view.characterWidth);
            double caretX = offset * view.characterWidth;
            double caretY = boundsInStackPane.getMinY();
            double caretBottom = boundsInStackPane.getMaxY();

            double x;
            double y;

            // vertical position: show below caret if there is enough space, otherwise show above
            if (caretBottom + CompletionPopupConstants.V_MARGIN + CompletionPopupConstants.HEIGHT
                    > getWrapperPane().getHeight()) {
                y = caretY - CompletionPopupConstants.V_MARGIN - CompletionPopupConstants.HEIGHT;
            } else {
                y = caretBottom + CompletionPopupConstants.V_MARGIN;
            }

            // horizontal position: align left edge with caret if there is enough space,
            // otherwise align right edge with caret
            if (caretX + CompletionPopupConstants.WIDTH > getWrapperPane().getWidth()) {
                x = caretX - CompletionPopupConstants.WIDTH;
            } else {
                x = caretX;
            }

            return new Point2D(x, y);
        }
    }

    private Font font;

    /**
     * Character width in text area. Required for message printer.
     */
    private double characterWidth;

    private final RichTextArea textArea = new CssRichTextArea();

    /**
     * The index of the paragraph with current prompt.
     */
    private TextPos promptPos;

    private final CommandHighlighter highlighter;

    private Node caret;

    private boolean textChangeValid = true;

    public ConsoleTabFxView(ShellFxView<?> shell) {
        super(shell);
        this.highlighter = createHighlighter();
    }

    @Override
    public void copy() {
        this.textArea.copy();
    }

    @Override
    public void paste() {
        this.textArea.paste();
    }

    @Override
    public void requestFocus() {
        NodeUtils.requestFocus(textArea);
    }

    @Override
    public void printPrompt(String prompt) {
        this.printTexts(List.of(new StyledText(StyleAttributeMap.EMPTY, prompt)), false); // adds a new paragraph
        this.promptPos = new TextPos(this.textArea.getParagraphCount() - 1, prompt.length(), 0, false);
        moveCaretToEnd();
    }

    @Override
    public void updatePrompt(String prompt) {
        var paragraphText = this.textArea.getModel().getParagraph(this.promptPos.index()).getPlainText();
        var start = new TextPos(this.promptPos.index(), 0, 0, false);
        var end = new TextPos(this.promptPos.index(), paragraphText.length(), 0, false);
        this.textArea.replaceText(start, end, prompt);
        this.promptPos = new TextPos(this.promptPos.index(), prompt.length(), 0, false);
        moveCaretToEnd();
    }

    @Override
    public void printMessages(List<Message> messages) {
        var texts = new ArrayList<StyledText>();
        for (var msg : messages) {
            var style = StyleAttributeMap.EMPTY;
            if (msg.getType() == MessageType.ERROR) {
                style = TextAreaCssStyles.ERROR;
            }
            var text = new StyledText(style, msg.getText());
            texts.add(text);
        }
        printTexts(texts, true);
    }

    @Override
    public void updateMonospaceFont(Font font) {
        this.font = font;
        this.characterWidth = StyleUtils.getMonospaceCharSize(font).getWidth();
    }

    @Override
    public void highlightCommands(Set<String> commands) {
        this.highlighter.setCommands(commands);
    }

    @Override
    public void replaceInput(String text) {
        int paraIndex = promptPos.index();
        int paraLen = textArea.getModel().getParagraph(paraIndex).getPlainText().length();
        TextPos paraEnd = new TextPos(paraIndex, paraLen, 0, false);
        textArea.replaceText(promptPos, paraEnd, text);
        moveCaretToEnd();
    }

    @Override
    public Composer getComposer() {
        return (Composer) super.getComposer();
    }

    @Override
    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void clear() {
        this.promptPos = null; // to prevent making text area non editable
        this.textArea.clear();
    }

    @Override
    protected void build() {
        super.build();
        var button = new Button("test");
        button.setOnAction(e -> getComposer().getShell().getPresenter().getContext()
                .getSettings().getAppearance().setTheme(AtlantaFxTheme.DRACULA));
        textArea.getStyleClass().addAll("console-text-area", StyleClasses.MONOSPACE);
        textArea.setEditable(true);
        textArea.setWrapText(true);
        textArea.setUndoRedoEnabled(false);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        var css = ConsoleTabFxView.class.getResource("console.css").toExternalForm();
        getWrapperPane().getStylesheets().add(css);
        getContentBox().getChildren().add(textArea);
    }

    @Override
    protected void addListeners() {
        super.addListeners();
        textArea.caretPositionProperty().addListener((ov, oldV, newV) -> {
            if (textArea.getSelection() == null) {
                textArea.setEditable(isInEditableRegion(newV));
            }
            updateCaretPos(newV);
        });
        textArea.selectionProperty().addListener((ov, oldV, newV) -> {
            if (newV != null && !Objects.equals(newV.getCaret(), newV.getAnchor())) {
                var caretIsInEditableRegion = isInEditableRegion(newV.getCaret());
                textArea.setEditable(isInEditableRegion(newV.getAnchor()) && caretIsInEditableRegion);
                getPresenter().onCopyAvailable(true);
            } else {
                textArea.setEditable(isInEditableRegion(newV.getCaret()));
                getPresenter().onCopyAvailable(false);
            }
            updateCaretPos(newV.getCaret());
        });
        textArea.getModel().addListener((change) -> {
            if (textChangeValid) {
                if (promptPos != null && change.getStart().index() == promptPos.index()
                        && change.getEnd().index() == promptPos.index()) {
                    getPresenter().onTextInput(textArea.getModel().getParagraph(promptPos.index()).getPlainText());
                    this.textChangeValid = false;
                    this.highlighter.highlight(promptPos.index());
                }
            }
            this.textChangeValid = true;
        });
    }

    @Override
    protected void addHandlers() {
        super.addHandlers();
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            var pos = textArea.getCaretPosition();
            if (isInEditableRegion(pos)) {
                if (event.getCode() == KeyCode.UP) {
                    if (textArea.getCaretPosition().index() == this.promptPos.index()) {
                        getPresenter().onMoveUp();
                        event.consume();
                    }
                } else if (event.getCode() == KeyCode.DOWN) {
                    if (textArea.getCaretPosition().index() == this.promptPos.index()) {
                        getPresenter().onMoveDown();
                        event.consume();
                    }
                } else if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (getComposer().completionPopup == null) {
                        textArea.appendText("\n");
                        getPresenter().onCommandsSubmitted(getCurrentParagraph(), calculateOutputWidth());
                    } else {
                        getPresenter().onElementSubmitted();
                    }

                } else if (event.getCode() == KeyCode.HOME) {
                    event.consume();
                    textArea.select(this.promptPos);
                } else if (event.getCode() == KeyCode.BACK_SPACE) {
                    if (pos.index() == promptPos.index() && pos.offset() == promptPos.offset()) {
                        event.consume();
                    }
                } else if (event.getCode() == KeyCode.SPACE) {
                    if (event.isControlDown()) {
                        getPresenter().onAutocomplete(getCurrentParagraph());
                        event.consume();
                    }
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    getPresenter().onPopupClose();
                    event.consume();
                }
            }
        });
//        cutItem.setOnAction((t) -> getTextArea().cut());
//        pasteItem.setOnAction((t) -> getTextArea().paste());
//        getTextArea().addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
//            cutItem.setDisable(!viewModel.isCutItemValid());
//            pasteItem.setDisable(!viewModel.isPasteItemValid());
//        });
    }

    protected Node getCaret() {
        if (this.caret == null) {
             this.caret = textArea.lookup(".caret");
        }
        return this.caret;
    }

    @Override
    protected Composer createComposer() {
        return new ConsoleTabFxView.Composer();
    }

    protected CommandHighlighter createHighlighter() {
        return new CommandHighlighter(textArea);
    }

    private void updateCaretPos(TextPos pos) {
        getPresenter().onCaretChanged(isInEditableRegion(pos) ? pos.offset() : null);
    }

    private String getCurrentParagraph() {
        return this.textArea.getModel().getParagraph(promptPos.index()).getPlainText();
    }

    private int calculateOutputWidth() {
        //vertical bar width is set in em, so, when font size changes vertical bar width changes too.
        var scrollWidth = 1.35 * font.getSize();
        var padding = 5;
        var r = Math.round((textArea.getWidth() - (padding + scrollWidth)) / this.characterWidth);
        return (int) r;
    }

    private boolean isInEditableRegion(TextPos pos) {
        if (promptPos == null) {
            return true;
        }
        var result = pos.index() == promptPos.index() && pos.offset() >= promptPos.offset();
        return result;
    }

    private void printTexts(List<StyledText> texts, boolean newLine) {
        for (var styledText : texts) {
            var style = styledText.getStyle();
            var text = styledText.getText();
            textArea.appendText(text, style);
            if (newLine) {
                textArea.appendText("\n");
                moveCaretToEnd();
            }
        }
    }

    private void moveCaretToEnd() {
        //textArea.select(textArea.getDocumentEnd(), textArea.getDocumentEnd());
        int lastPara = textArea.getParagraphCount() - 1;
        lastPara = Math.max(0, lastPara);
        String lastText = textArea.getModel().getPlainText(lastPara);
        TextPos end = TextPos.ofLeading(lastPara, lastText.length());
        textArea.select(end);
    }

}
