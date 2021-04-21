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

package net.shadew.lodemc.loader.impl.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.PhaseStatus;

public class ModDiscoveryPhase implements LoaderPhase<List<ModFile>> {
    public static final ModDiscoveryPhase INSTANCE = new ModDiscoveryPhase();

    private final List<ModFile> pendingFiles = new ArrayList<>();
    private final List<ModFile> files = new ArrayList<>();
    private final Map<String, List<LoaderException>> failedPaths = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    @Override
    public String name() {
        return "ModDiscovery";
    }

    @Override
    public List<ModFile> result() {
        return files;
    }

    @Override
    public PhaseStatus status() {
        return status;
    }

    @Override
    public Map<String, List<LoaderException>> problems() {
        return failedPaths;
    }

    @Override
    public void run(LodeLoader loader) {
        for (Path folder : loader.getModsFolders()) {
            discover(folder);
        }
    }

    private void discover(Path modsPath) {
        discover(modsPath, null);

        while (!pendingFiles.isEmpty()) {
            ModFile file = pendingFiles.remove(0);

            Path inclusionsPath = file.findFile("inclusions/");
            try {
                discover(inclusionsPath, file);
                files.add(file);
            } catch (LoaderException exc) {
                handleProblem(file.getContainer().getFilePath(), exc);
            }
        }
    }

    private void discover(Path path, ModFile owner) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> files = Files.list(path)) {
            files.forEach(filePath -> {
                try {
                    pendingFiles.add(new ModFile(filePath, owner));
                } catch (LoaderException exc) {
                    if (owner == null)
                        handleProblem(filePath, exc);
                    else // Rethrow, discover(Path) will catch it and handle it for the correct path
                        throw exc;
                }
            });
        } catch (IOException exc) {
            throw new LoaderException("Exception locating files in directory " + path, exc).problem(owner);
        }
    }

    private void handleProblem(Path path, LoaderException problem) {
        failedPaths.computeIfAbsent(path.toString(), k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }
}
