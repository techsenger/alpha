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

package com.techsenger.weaverbird.gui.session;

import com.techsenger.shellfx.core.area.AbstractAreaPresenter;
import com.techsenger.weaverbird.net.client.api.ClientService;
import com.techsenger.weaverbird.net.client.api.ClientSession;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Pavel Castornii
 */
public abstract class AbstractSessionToolBarPresenter<V extends SessionToolBarView>
        extends AbstractAreaPresenter<V> {

    private final ClientService client;

    private List<ClientSession> sessions;

    private ClientSession session;

    private SessionToolBarParams params;

    public AbstractSessionToolBarPresenter(V view, SessionToolBarParams params) {
        super(view, params);
        this.client = params.getClient();
        this.params = params;
    }

    public List<ClientSession> getSessions() {
        return this.sessions;
    }

    public ClientSession getSession() {
        return this.session;
    }

    @Override
    protected void postInitialize() {
        super.postInitialize();
        var clientSessions = getClientSessions();
        if (clientSessions != null) {
            setSessions(clientSessions);
        }
        setSession(params.getSession());
        this.params = null;
    }

    protected void onSessionChanged(ClientSession session) {
        this.session = session;
    }

    protected void onRefresh() {
        var clientSessions = getClientSessions();
        if (clientSessions != null) {
            setSessions(clientSessions);
        }
        setSession(session);
    }

    protected void setSessions(List<ClientSession> sessions) {
        this.sessions = sessions;
        getView().updateSessions(sessions);
    }

    protected void setSession(ClientSession session) {
        if (Objects.equals(this.session, session)) {
            return;
        }
        this.session = session;
        getView().updateSession(session);
    }

    protected List<ClientSession> getClientSessions() {
        if (this.client == null) {
            return null;
        }
        var sessions = client.getSessionsByName().values().stream()
                .sorted(Comparator.comparing(ClientSession::getName)).toList();
        return sessions;
    }
}
