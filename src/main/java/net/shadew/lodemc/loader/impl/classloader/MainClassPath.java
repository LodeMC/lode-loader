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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.shadew.lodemc.loader.api.transformer.ClassPath;
import net.shadew.util.misc.IO;

public class MainClassPath implements ClassPath {
    private final ClassLoader loader;

    public MainClassPath(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public Iterable<URL> findSources(String path) {
        List<URL> urls = new ArrayList<>();
        try {
            Enumeration<URL> found = loader.getResources(path);
            while (found.hasMoreElements())
                urls.add(found.nextElement());
        } catch (IOException ignored) {
        }
        return urls;
    }

    @Override
    public byte[] loadClassBytes(String className) throws ClassNotFoundException {
        URL url = loader.getResource(className.replace('.', '/') + ".class");
        if (url == null)
            throw new ClassNotFoundException(className);
        try (InputStream in = url.openStream()) {
            return IO.readAll(in);
        } catch (IOException e) {
            throw new ClassNotFoundException(className);
        }
    }
}
