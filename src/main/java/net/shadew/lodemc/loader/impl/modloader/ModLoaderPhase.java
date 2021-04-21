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
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.transformer.ClassPath;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.LodeLoaderImpl;
import net.shadew.lodemc.loader.impl.PhaseStatus;
import net.shadew.lodemc.loader.impl.discovery.ModFile;

public class ModLoaderPhase implements LoaderPhase<List<ModImpl>> {
    private final Map<String, List<LoaderException>> problems = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    private final Set<String> usedIds = new HashSet<>();
    private final List<ModImpl> result = new ArrayList<>();
    private final List<ModFile> modFiles;

    public ModLoaderPhase(List<ModFile> modFiles) {
        this.modFiles = modFiles;
        usedIds.add("minecraft");
    }

    @Override
    public String name() {
        return "ModLoading";
    }

    @Override
    public List<ModImpl> result() {
        return result;
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
        for (ModFile modFile : modFiles) {
            Path modJson = modFile.findFile("lode_mod.json");

            if (Files.exists(modJson) && Files.isRegularFile(modJson)) {
                try {
                    ModConfigImpl config = loadConfig(modJson, modFile);
                    String id = config.getId();
                    if (usedIds.contains(id)) {
                        throw new LoaderException("Mod with '" + id + "' exists twice").critical().problem(modFile);
                    }
                    usedIds.add(id);

                    if (config.canLoadInEnv(loader.getEnvironment())) {
                        ModImpl mod = new ModImpl(config.getId(), modFile, config);
                        result.add(mod);
                    }
                } catch (LoaderException exc) {
                    handleProblem(modFile.getFilePath().toString(), exc.critical().problem(modFile));
                }
            }
        }

        List<ClassPath> defaults = LodeLoaderImpl.instance.getDefaultClassPaths();

        for (ClassPath path : defaults) {
            for (URL url : path.findSources("lode_mod.json")) {
                try {
                    Path urlPath = Paths.get(url.toURI());
                    ModConfigImpl config = loadConfig(urlPath, url);
                    String id = config.getId();
                    if (usedIds.contains(id)) {
                        throw new LoaderException("Mod with '" + id + "' exists twice").critical().problem(url);
                    }
                    usedIds.add(id);

                    if (config.canLoadInEnv(loader.getEnvironment())) {
                        ModImpl mod = new ModImpl(config.getId(), path, urlPath.getParent(), config);
                        result.add(mod);
                    }
                } catch (URISyntaxException e) {
                    handleProblem(url.toString(), new LoaderException(e).critical().problem(url));
                } catch (LoaderException exc) {
                    handleProblem(url.toString(), exc.critical().problem(url));
                }
            }
        }
    }

    private ModConfigImpl loadConfig(Path path, Object responsible) {
        JsonElement element;
        try (InputStreamReader in = new InputStreamReader(Files.newInputStream(path))) {
            element = new JsonParser().parse(in);
        } catch (Exception exc) {
            throw new LoaderException("Failed to load lode_mod.json", exc).critical().problem(responsible);
        }

        try {
            return ModConfigImpl.load(element.getAsJsonObject());
        } catch (LoaderException exc) {
            throw exc.problem(responsible);
        } catch (Exception exc) {
            throw new LoaderException("Failed to load lode_mod.json", exc).critical().problem(responsible);
        }
    }

    private void handleProblem(String type, LoaderException problem) {
        problems.computeIfAbsent(type, k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }
}
