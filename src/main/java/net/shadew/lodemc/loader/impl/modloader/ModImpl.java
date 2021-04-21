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

import org.objectweb.asm.Type;

import java.nio.file.Path;
import java.util.*;

import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.mod.AnnotatedElement;
import net.shadew.lodemc.loader.api.mod.Mod;
import net.shadew.lodemc.loader.api.transformer.ClassPath;
import net.shadew.lodemc.loader.impl.discovery.ModFile;
import net.shadew.lodemc.loader.impl.transformers.CoLoadInjector;

public class ModImpl implements Mod {
    private final String id;
    private final ClassPath classpath;
    private final ModConfigImpl config;
    private final Path rootPath;
    private final List<AnnotatedElementImpl> annotatedElements = new ArrayList<>();
    private final List<AnnotatedElement> annotatedElementsImm = Collections.unmodifiableList(annotatedElements);
    private final Map<Type, List<AnnotatedElementImpl>> elementsByAnnotation = new HashMap<>();
    private final Map<Type, List<AnnotatedElement>> elementsByAnnotationImm = new HashMap<>();
    private final List<EntrypointContainer> entrypoints = new ArrayList<>();

    public ModImpl(String id, ModFile modFile, ModConfigImpl config) {
        this.id = id;
        this.classpath = modFile;
        this.config = config;
        this.rootPath = modFile.getRoot();
    }

    public ModImpl(String id, ClassPath classpath, Path rootPath, ModConfigImpl config) {
        this.id = id;
        this.classpath = classpath;
        this.config = config;
        this.rootPath = rootPath;
    }

    @Override
    public String getId() {
        return id;
    }

    public ClassPath getClassPath() {
        return classpath;
    }

    @Override
    public ModConfigImpl getConfig() {
        return config;
    }

    @Override
    public Path getPath(String path) {
        return rootPath.resolve(path);
    }

    @Override
    public Path getRootPath() {
        return rootPath;
    }

    @Override
    public List<AnnotatedElement> getAnnotatedElements(Type annotationType) {
        return elementsByAnnotationImm.computeIfAbsent(
            annotationType,
            k -> Collections.unmodifiableList(elementsByAnnotation.get(k))
        );
    }

    @Override
    public List<AnnotatedElement> getAnnotatedElements() {
        return annotatedElementsImm;
    }

    public void runEntrypoints(LodeLoader loader) {
        entrypoints.forEach(entrypoint -> {
            if (entrypoint.isSafeToLoad(loader))
                entrypoint.call(loader);
        });
    }

    public void addAnnotatedElement(AnnotatedElementImpl elm) {
        annotatedElements.add(elm);
        elementsByAnnotation.computeIfAbsent(elm.getAnnotationType(), k -> new ArrayList<>()).add(elm);

        EntrypointContainer entrypoint = EntrypointContainer.tryInstantiate(elm);
        if (entrypoint != null) entrypoints.add(entrypoint);

        CoLoadInjector.process(elm);
    }
}
