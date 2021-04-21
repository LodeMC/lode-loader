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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.plugin.LoaderPlugin;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.PhaseStatus;

public class PluginInitPhase implements LoaderPhase<Void> {
    private final Map<String, List<LoaderException>> problems = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    private final Map<String, List<LoaderPlugin>> plugins;
    private final BiConsumer<LoaderPlugin, LodeLoader> initializer;

    public PluginInitPhase(Map<String, List<LoaderPlugin>> plugins, BiConsumer<LoaderPlugin, LodeLoader> initializer) {
        this.plugins = plugins;
        this.initializer = initializer;
    }

    @Override
    public String name() {
        return "InitializePlugins";
    }

    @Override
    public Void result() {
        return null;
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
        plugins.forEach((id, plugins) -> plugins.forEach(plugin -> {
            try {
                initializer.accept(plugin, loader);
            } catch (LoaderException exc) {
                handleProblem(id, exc);
            }
        }));
    }

    private void handleProblem(String type, LoaderException problem) {
        problems.computeIfAbsent(type, k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }
}
