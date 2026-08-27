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

package com.techsenger.weaverbird.core.impl.component;

import com.techsenger.weaverbird.core.api.component.ComponentConfig;
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.RepositoryConfig;
import com.techsenger.weaverbird.core.api.module.DirectiveType;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.EXPORTS;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.OPENS;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.READS;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.REQUESTS_EXPORT;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.REQUESTS_OPEN;
import static com.techsenger.weaverbird.core.api.module.DirectiveType.REQUESTS_READ;
import com.techsenger.weaverbird.core.api.module.ModuleConfig;
import com.techsenger.weaverbird.core.api.module.ModuleDirective;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Pavel Castornii
 */
public final class ConfigXmlWriter {

    private static final String ENCODING = "UTF-8";
    private static final String XML_VERSION = "1.0";
    private static final int INDENT = 4;

    public void write(ComponentConfig config, Writer writer) throws Exception {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        var factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        var handler = factory.newTransformerHandler();
        var transformer = handler.getTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(INDENT));
        handler.setResult(new StreamResult(writer));
        handler.startDocument();
        writeConfiguration(handler, config);
        handler.endDocument();
    }

    private void writeConfiguration(TransformerHandler handler, ComponentConfig config) throws SAXException {
        var attrs = new AttributesImpl();
        addAttributeIfNotNull(attrs, "name", config.getName());
        addAttributeIfNotNull(attrs, "version", config.getVersion() != null
                ? config.getVersion().toString() : null);
        addAttributeIfNotNull(attrs, "type", config.getType());
        handler.startElement("", "", Tags.CONFIGURATION_TAG, attrs);
        writeMetadata(handler, config.getMetadata());
        writeRepositories(handler, config.getRepositories());
        writeParents(handler, config.getParents());
        writeModules(handler, config.getModules());
        handler.endElement("", "", Tags.CONFIGURATION_TAG);
    }

    private void writeMetadata(TransformerHandler handler, Map<String, String> metadata) throws SAXException {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        handler.startElement("", "", Tags.METADATA_TAG, new AttributesImpl());
        for (var entry : metadata.entrySet()) {
            var attrs = new AttributesImpl();
            attrs.addAttribute("", "", "key", "CDATA", entry.getKey());
            attrs.addAttribute("", "", "value", "CDATA", entry.getValue());
            handler.startElement("", "", Tags.ENTRY_TAG, attrs);
            handler.endElement("", "", Tags.ENTRY_TAG);
        }
        handler.endElement("", "", Tags.METADATA_TAG);
    }

    private void writeRepositories(TransformerHandler handler, List<RepositoryConfig> repositories)
            throws SAXException {
        if (repositories == null || repositories.isEmpty()) {
            return;
        }
        handler.startElement("", "", "Repositories", new AttributesImpl());
        for (var repo : repositories) {
            var attrs = new AttributesImpl();
            addAttributeIfNotNull(attrs, "name", repo.getName());
            addAttributeIfNotNull(attrs, "url", repo.getUrl());
            handler.startElement("", "", Tags.REPOSITORY_TAG, attrs);
            handler.endElement("", "", Tags.REPOSITORY_TAG);
        }
        handler.endElement("", "", "Repositories");
    }

    private void writeParents(TransformerHandler handler, List<ParentConfig> parents) throws SAXException {
        if (parents == null || parents.isEmpty()) {
            return;
        }
        handler.startElement("", "", "Parents", new AttributesImpl());
        for (var parent : parents) {
            var attrs = new AttributesImpl();
            addAttributeIfNotNull(attrs, "name", parent.getName());
            addAttributeIfNotNull(attrs, "version", parent.getVersion() != null
                    ? parent.getVersion().toString() : null);
            addAttributeIfNotNull(attrs, "versionMatch", parent.getVersionMatch() != null
                    ? parent.getVersionMatch().name().toLowerCase() : null);
            if (parent.isOptional()) {
                attrs.addAttribute("", "", "optional", "CDATA", "true");
            }
            handler.startElement("", "", Tags.PARENT_TAG, attrs);
            handler.endElement("", "", Tags.PARENT_TAG);
        }
        handler.endElement("", "", "Parents");
    }

    private void writeModules(TransformerHandler handler, List<ModuleConfig> modules) throws SAXException {
        if (modules == null || modules.isEmpty()) {
            return;
        }
        handler.startElement("", "", "Modules", new AttributesImpl());
        for (var module : modules) {
            writeModule(handler, module);
        }
        handler.endElement("", "", "Modules");
    }

    private void writeModule(TransformerHandler handler, ModuleConfig module) throws SAXException {
        var attrs = new AttributesImpl();
        addAttributeIfNotNull(attrs, "groupId", module.getGroupId());
        addAttributeIfNotNull(attrs, "artifactId", module.getArtifactId());
        addAttributeIfNotNull(attrs, "version", module.getVersion().toString());
        addAttributeIfNotNull(attrs, "classifier", module.getClassifier());
        addAttributeIfNotNull(attrs, "type", module.getType() != null
                ? module.getType().toString().toLowerCase() : null);
        if (module.isActive()) {
            attrs.addAttribute("", "", "active", "CDATA", "true");
        }
        if (module.isNativeAccessEnabled()) {
            attrs.addAttribute("", "", "nativeAccessEnabled", "CDATA", "true");
        }
        var directives = module.getDirectives();
        boolean hasDirectives = directives != null && !directives.isEmpty();
        handler.startElement("", "", Tags.MODULE_TAG, attrs);
        if (hasDirectives) {
            writeDirectives(handler, directives);
        }
        handler.endElement("", "", Tags.MODULE_TAG);
    }

    private void writeDirectives(TransformerHandler handler, List<ModuleDirective> directives) throws SAXException {
        handler.startElement("", "", Tags.DIRECTIVES_TAG, new AttributesImpl());
        for (var directive : directives) {
            var attrs = new AttributesImpl();
            addAttributeIfNotNull(attrs, "type", resolveDirectiveType(directive.getType()));
            addAttributeIfNotNull(attrs, "package", directive.getPackage());
            addAttributeIfNotNull(attrs, "module", directive.getModule());
            addAttributeIfNotNull(attrs, "layer", directive.getLayer());
            handler.startElement("", "", Tags.DIRECTIVE_TAG, attrs);
            handler.endElement("", "", Tags.DIRECTIVE_TAG);
        }
        handler.endElement("", "", Tags.DIRECTIVES_TAG);
    }

    private String resolveDirectiveType(DirectiveType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case OPENS -> "opens";
            case READS -> "reads";
            case EXPORTS -> "exports";
            case REQUESTS_OPEN -> "requestsOpen";
            case REQUESTS_READ -> "requestsRead";
            case REQUESTS_EXPORT -> "requestsExport";
        };
    }

    private void addAttributeIfNotNull(AttributesImpl attrs, String name, String value) {
        if (value != null) {
            attrs.addAttribute("", "", name, "CDATA", value);
        }
    }
}
