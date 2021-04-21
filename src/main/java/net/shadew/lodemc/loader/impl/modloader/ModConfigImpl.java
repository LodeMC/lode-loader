/*
 * Copyright 2021 Shadew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shadew.lodemc.loader.impl.modloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.shadew.lodemc.loader.api.Environment;
import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.mod.ModConfig;

public class ModConfigImpl implements ModConfig {
    private final String id;
    private final Environment environment;
    private String name;
    private String description;
    private String version;
    private String license;
    private final List<String> authors = new ArrayList<>();
    private final List<String> credits = new ArrayList<>();
    private final Map<String, String> contactLinks = new LinkedHashMap<>();
    private final Map<String, List<String>> mixinConfigs = new LinkedHashMap<>();

    private final List<String> entrypoints = new ArrayList<>();

    public ModConfigImpl(String id, Environment environment) {
        this.id = id;
        this.environment = environment;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean canLoadInEnv(Environment env) {
        return environment == null || env == environment;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void setName(JsonElement element) {
        if (element == null) {
            throw new LoaderException("Field 'name' not specified");
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new LoaderException("Field 'name' must be a string");
        }
        setName(element.getAsString());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private void setVersion(JsonElement element) {
        if (element == null) {
            throw new LoaderException("Field 'version' not specified");
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new LoaderException("Field 'version' must be a string");
        }
        setVersion(element.getAsString());
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private void setDescription(JsonElement element) {
        if (element == null) {
            return;
        }

        if (!element.isJsonPrimitive()) {
            if (element.isJsonArray()) {
                boolean space = false;
                StringBuilder builder = new StringBuilder();
                for (JsonElement el : element.getAsJsonArray()) {
                    if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                        throw new LoaderException("Field 'description' must be a string or an array of strings");
                    }
                    if (space)
                        builder.append(" ");
                    space = true;

                    builder.append(el.getAsString());
                }
                setDescription(builder.toString());
                return;
            }

            throw new LoaderException("Field 'description' must be a string or an array of strings");
        }

        if (!element.getAsJsonPrimitive().isString())
            throw new LoaderException("Field 'description' must be a string or an array of strings");

        setDescription(element.getAsString());
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void addAuthor(String author) {
        authors.add(author);
    }

    private void setAuthors(JsonElement element) {
        if (element == null) {
            return;
        }

        if (!element.isJsonArray()) {
            throw new LoaderException("Field 'authors' must be an array of strings");
        }

        for (JsonElement el : element.getAsJsonArray()) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                throw new LoaderException("Field 'authors' must be an array of strings");
            }

            addAuthor(el.getAsString());
        }
    }

    @Override
    public List<String> getAuthors() {
        return authors;
    }

    public void addContactLink(String name, String link) {
        contactLinks.put(name, link);
    }

    private void setContactLinks(JsonElement element) {
        if (element == null) {
            return;
        }

        if (!element.isJsonObject()) {
            throw new LoaderException("Field 'contact' must be an object with string values");
        }

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            JsonElement el = entry.getValue();
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                throw new LoaderException("Field 'contact' must be an object with string values");
            }

            addContactLink(key, el.getAsString());
        }
    }

    @Override
    public Map<String, String> getContactLinks() {
        return contactLinks;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    private void setLicense(JsonElement element) {
        if (element == null) {
            throw new LoaderException("Field 'license' not specified");
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new LoaderException("Field 'license' must be a string");
        }
        setVersion(element.getAsString());
    }

    @Override
    public String getLicense() {
        return license;
    }

    public void addCredit(String credit) {
        credits.add(credit);
    }

    private void setCredits(JsonElement element) {
        if (element == null) {
            return;
        }

        if (!element.isJsonArray()) {
            throw new LoaderException("Field 'credits' must be an array of strings");
        }

        for (JsonElement el : element.getAsJsonArray()) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                throw new LoaderException("Field 'credits' must be an array of strings");
            }

            addCredit(el.getAsString());
        }
    }

    @Override
    public List<String> getCredits() {
        return credits;
    }

    public void addMixinConfig(String mod, String cfg) {
        mixinConfigs.computeIfAbsent(mod, k -> new ArrayList<>()).add(cfg);
    }

    private void setMixinConfigs(JsonElement element) {
        if (element == null) {
            return;
        }

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                JsonElement el = entry.getValue();
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    addMixinConfig(key, el.getAsString());
                } else if (el.isJsonArray()) {
                    for (JsonElement el1 : el.getAsJsonArray()) {
                        if (!el1.isJsonPrimitive() || !el1.getAsJsonPrimitive().isJsonArray()) {
                            throw new LoaderException("Field 'mixins' must contain strings or arrays of strings");
                        }

                        addMixinConfig(key, el1.getAsString());
                    }
                }

                throw new LoaderException("Field 'mixins' must contain strings or arrays of strings");
            }
            return;
        } else if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isJsonArray()) {
                    throw new LoaderException("Field 'mixins' must be an object, an array of strings or a string");
                }

                addMixinConfig("minecraft", el.getAsString());
            }
            return;
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            addMixinConfig("minecraft", element.getAsString());
            return;
        }

        throw new LoaderException("Field 'mixins' must be an object, an array of strings or a string");
    }

    @Override
    public Map<String, List<String>> getMixinConfigs() {
        return mixinConfigs;
    }

    public void addEntrypoint(String cls) {
        entrypoints.add(cls);
    }

    private void setEntrypoints(JsonElement element) {
        if (element == null) {
            return;
        }

        if (!element.isJsonArray()) {
            throw new LoaderException("Field 'entrypoints' must be an array of strings");
        }

        for (JsonElement el : element.getAsJsonArray()) {
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
                throw new LoaderException("Field 'entrypoints' must be an array of strings");
            }

            addEntrypoint(el.getAsString());
        }
    }

    @Override
    public List<String> getEntrypoints() {
        return entrypoints;
    }

    public static ModConfigImpl load(JsonObject object) {
        JsonElement idElement = object.get("id");
        if (idElement == null) {
            throw new LoaderException("Field 'id' not specified");
        }

        if (!idElement.isJsonPrimitive() || !idElement.getAsJsonPrimitive().isString()) {
            throw new LoaderException("Field 'id' must be a string");
        }
        String id = idElement.getAsString();

        JsonElement envElement = object.get("environment");
        Environment env = null;
        if (envElement != null) {
            if (!envElement.isJsonPrimitive() || !envElement.getAsJsonPrimitive().isString()) {
                throw new LoaderException("Field 'environment' must be a string");
            }
            String envs = envElement.getAsString();
            switch (envs) {
                case "client":
                    env = Environment.CLIENT;
                    break;
                case "server":
                    env = Environment.SERVER;
                    break;
                case "both":
                    env = null;
                    break;
                default:
                    throw new LoaderException("Invalid environment '" + envs + "', must be 'client', 'server' or 'both'");
            }
        }

        ModConfigImpl impl = new ModConfigImpl(id, env);

        impl.setName(object.get("name"));
        impl.setVersion(object.get("version"));
        impl.setDescription(object.get("description"));
        impl.setAuthors(object.get("authors"));
        impl.setContactLinks(object.get("contact"));
        impl.setLicense(object.get("license"));
        impl.setCredits(object.get("credits"));
        impl.setEntrypoints(object.get("entrypoints"));
        impl.setMixinConfigs(object.get("mixins"));

        return impl;
    }
}
