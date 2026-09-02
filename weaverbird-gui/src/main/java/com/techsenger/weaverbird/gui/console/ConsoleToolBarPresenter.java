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

import com.techsenger.weaverbird.gui.session.AbstractSessionToolBarPresenter;
import com.techsenger.weaverbird.net.client.api.ClientSession;

/**
 *
 * @author Pavel Castornii
 */
public class ConsoleToolBarPresenter<V extends ConsoleToolBarView> extends AbstractSessionToolBarPresenter<V>
        implements ConsoleToolBarPort {

    private final ConsoleToolBarAwarePort toolBarAware;

    private boolean copyDisabled = false;

    private boolean pasteDisabled = false;

    public ConsoleToolBarPresenter(V view, ConsoleToolBarParams params) {
        super(view, params);
        this.toolBarAware = params.getToolBarAware();
    }

    @Override
    public void updateSession(ClientSession session) {
        setSessions(getClientSessions());
        setSession(session);
    }

    @Override
    public void onCopyAvailable(boolean value) {
        setCopyDisabled(!value);
    }

    public boolean isCopyDisabled() {
        return copyDisabled;
    }

    public boolean isPasteDisabled() {
        return pasteDisabled;
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        setCopyDisabled(true);
        // setPasteDisabled(true);
    }

    protected void onClear() {
        this.toolBarAware.onClear();
    }

    protected void onCopy() {
        this.toolBarAware.onCopy();
    }

    protected void onPaste() {
        this.toolBarAware.onPaste();
    }

    protected void setCopyDisabled(boolean copyDisabled) {
        if (this.copyDisabled == copyDisabled) {
            return;
        }
        this.copyDisabled = copyDisabled;
        getView().updateCopyDisabled(copyDisabled);
    }

    protected void setPasteDisabled(boolean pasteDisabled) {
        if (this.pasteDisabled == pasteDisabled) {
            return;
        }
        this.pasteDisabled = pasteDisabled;
        getView().updatePasteDisabled(pasteDisabled);
    }

    @Override
    protected void onSessionChanged(ClientSession session) {
        super.onSessionChanged(session);
        this.toolBarAware.onSessionChanged(session);
    }
}
