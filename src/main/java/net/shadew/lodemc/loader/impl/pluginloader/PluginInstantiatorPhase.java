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

import java.util.*;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.plugin.LoaderPlugin;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.LodeLoaderImpl;
import net.shadew.lodemc.loader.impl.PhaseStatus;

public class PluginInstantiatorPhase implements LoaderPhase<Map<String, List<LoaderPlugin>>> {
    private final List<PluginInfo> pluginInfos;
    private final Set<String> loaded = new HashSet<>();
    private final Map<String, List<LoaderPlugin>> plugins = new LinkedHashMap<>();
    private final Map<String, List<LoaderException>> problems = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    public PluginInstantiatorPhase(List<PluginInfo> pluginInfos) {
        this.pluginInfos = pluginInfos;
    }

    @Override
    public String name() {
        return "InstantiatePlugins";
    }

    @Override
    public Map<String, List<LoaderPlugin>> result() {
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
        ClassLoader clsLoader = LodeLoaderImpl.instance.getAgentClassLoader();
        for (PluginInfo info : pluginInfos) {
            for (String cls : info.getPluginEntrypointClasses()) {
                if (loaded.contains(cls)) continue;
                LoaderPlugin plugin = load(info, cls, clsLoader);
                if (plugin != null) {
                    plugins.computeIfAbsent(info.getPluginId(), k -> new ArrayList<>()).add(plugin);
                    loaded.add(cls);
                }
            }
        }
    }

    private LoaderPlugin load(PluginInfo pluginInfo, String clsName, ClassLoader loader) {
        try {
            Class<?> cls = loader.loadClass(clsName);
            return (LoaderPlugin) cls.newInstance();
        } catch (ClassNotFoundException e) {
            handleProblem(pluginInfo.getPluginId(), new LoaderException("No such plugin class: " + clsName).critical().problem(pluginInfo));
        } catch (InstantiationException e) {
            handleProblem(pluginInfo.getPluginId(), new LoaderException("Could not instantiate: " + clsName).critical().problem(pluginInfo));
        } catch (IllegalAccessException e) {
            handleProblem(pluginInfo.getPluginId(), new LoaderException("Could not access constructor of: " + clsName).critical().problem(pluginInfo));
        } catch (ClassCastException e) {
            handleProblem(pluginInfo.getPluginId(), new LoaderException("Not a LoaderPlugin implementation: " + clsName).critical().problem(pluginInfo));
        }
        return null;
    }

    private void handleProblem(String type, LoaderException problem) {
        problems.computeIfAbsent(type, k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }
}
