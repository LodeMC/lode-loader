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

package net.shadew.lodemc.loader.impl.classloader;

import java.net.URL;
import java.util.*;

import net.shadew.lodemc.loader.api.transformer.ClassPath;
import net.shadew.lodemc.loader.api.transformer.Transformer;

public class ExtendableClassLoader extends ClassLoader {
    private final Set<ClassPath> classPaths = new LinkedHashSet<>();
    private final Set<Transformer> transformers = new LinkedHashSet<>();
    private final Set<Transformer> postTransformer = new LinkedHashSet<>();

    public ExtendableClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addClassPath(ClassPath path) {
        classPaths.add(path);
    }

    public void addTransformer(Transformer transformer) {
        transformers.add(transformer);
    }

    public void addPostTransformer(Transformer transformer) {
        postTransformer.add(transformer);
    }

    public byte[] getClassBytes(String internalName) throws ClassNotFoundException {
        byte[] bytes = null;
        for (ClassPath path : classPaths) {
            try {
                bytes = path.loadClassBytes(internalName);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (bytes == null) {
            throw new ClassNotFoundException(internalName);
        }
        for (Transformer transformer : transformers) {
            if (transformer.canTransform(internalName)) {
                bytes = transformer.transformClassBytes(internalName, bytes);
                if (bytes == null) {
                    throw new ClassNotFoundException(internalName);
                }
            }
        }
        return bytes;
    }

    protected byte[] postTransform(String name, byte[] bytes) throws ClassNotFoundException {
        for (Transformer transformer : postTransformer) {
            if (transformer.canTransform(name)) {
                bytes = transformer.transformClassBytes(name, bytes);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
            }
        }
        return bytes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/');
        byte[] bytes = getClassBytes(internalName);
        bytes = postTransform(name, bytes);
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected String findLibrary(String libname) {
        return null;
    }

    @Override
    protected URL findResource(String name) {
        for (ClassPath path : classPaths) {
            for (URL url : path.findSources(name)) {
                return url; // At the instant there is a class path with this url we return it
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        List<URL> result = new ArrayList<>();
        for (ClassPath path : classPaths) {
            for (URL url : path.findSources(name)) {
                result.add(url);
            }
        }
        return Collections.enumeration(result);
    }
}
