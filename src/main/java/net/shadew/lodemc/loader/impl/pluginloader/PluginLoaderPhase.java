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

package net.shadew.lodemc.loader.impl.pluginloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.LodeLoaderImpl;
import net.shadew.lodemc.loader.impl.PhaseStatus;

public class PluginLoaderPhase implements LoaderPhase<List<PluginInfo>> {
    public static final PluginLoaderPhase INSTANCE = new PluginLoaderPhase();

    private final List<PluginInfo> plugins = new ArrayList<>();
    private final Set<String> usedIds = new HashSet<>();
    private final Map<String, List<LoaderException>> problems = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    public PluginLoaderPhase() {
        usedIds.add("minecraft");
    }

    @Override
    public String name() {
        return "PluginLoader";
    }

    @Override
    public List<PluginInfo> result() {
        return plugins;
    }

    @Override
    public PhaseStatus status() {
        return status;
    }

    @Override
    public Map<String, List<LoaderException>> problems() {
        return problems;
    }

    @Override
    public void run(LodeLoader loader) {
        ClassLoader classLoader = LodeLoaderImpl.instance.getAgentClassLoader();
        try {
            Enumeration<URL> pluginJsons = classLoader.getResources("lode_loader_plugin.json");
            while (pluginJsons.hasMoreElements()) {
                URL url = pluginJsons.nextElement();
                try {
                    loadPluginInfo(url);
                } catch (LoaderException exc) {
                    handleProblem(url.toString(), exc);
                }
            }
        } catch (IOException e) {
            handleProblem("loading", new LoaderException("Failed to load lode_loader_plugin.json files", e).critical());
        }
    }

    private void loadPluginInfo(URL url) {
        try (Reader reader = new InputStreamReader(url.openStream())) {
            JsonElement element = new JsonParser().parse(reader);
            loadPluginInfo(element, url);
        } catch (LoaderException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new LoaderException("Failed to load plugin info at " + url).critical().problem(url);
        }
    }

    private void loadPluginInfo(JsonElement json, URL url) {
        if (!json.isJsonObject())
            throw new LoaderException("Root of lode_loader_plugin.json is not an object").critical().problem(url);
        JsonObject obj = json.getAsJsonObject();

        String id = requireString(obj, "id", url);
        String pkg = requireString(obj, "package", url);

        JsonElement pluginsElm = obj.get("plugins");
        if (pluginsElm == null)
            throw new LoaderException("Missing 'plugins'").critical().problem(url);
        if (!pluginsElm.isJsonArray())
            throw new LoaderException("'plugins' not an array").critical().problem(url);

        PluginInfo info = new PluginInfo(id, pkg);
        for (JsonElement element : pluginsElm.getAsJsonArray()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                info.addPluginEntrypointClass(pkg + "." + element.getAsString());
            } else {
                throw new LoaderException("'plugins' must contain only strings").critical().problem(url);
            }
        }

        add(info, url);
    }

    private void add(PluginInfo info, URL url) {
        if (usedIds.contains(info.getPluginId())) {
            throw new LoaderException("Plugin id '" + info.getPluginId() + "' defined twice").critical().problem(url);
        }
        plugins.add(info);
        usedIds.add(info.getPluginId());
    }

    private String requireString(JsonObject object, String key, URL url) {
        if (!object.has(key))
            throw new LoaderException("Missing '" + key + "'").critical().problem(url);

        JsonElement val = object.get(key);
        if (!val.isJsonPrimitive() || !val.getAsJsonPrimitive().isString())
            throw new LoaderException("'" + key + "' not a string").critical().problem(url);

        return val.getAsString();
    }

    private void handleProblem(String type, LoaderException problem) {
        problems.computeIfAbsent(type, k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }
}
