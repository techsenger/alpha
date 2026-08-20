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

import com.techsenger.shellfx.core.popup.PopupView;
import java.util.List;

/**
 *
 * @author Pavel Castornii
 */
public interface CompletionPopupView extends PopupView {

    void updateItems(List<CompletionItem<?>> items);

    void displayInfo(String description, String module);

    void displayInfo(String description, boolean required, String alias);

    void selectPrevious();

    void selectNext();
}
