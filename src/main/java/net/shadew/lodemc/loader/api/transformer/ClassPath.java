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

package net.shadew.lodemc.loader.api.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.shadew.util.misc.IO;

public interface ClassPath {
    Iterable<URL> findSources(String path);

    default byte[] loadClassBytes(String className) throws ClassNotFoundException {
        URL resource = null;
        Iterable<URL> sources = findSources(className + ".class");
        for (URL url : sources) {
            if (url != null) {
                resource = url;
                break;
            }
        }
        if (resource == null) {
            throw new ClassNotFoundException(className);
        }
        try (InputStream in = resource.openStream()) {
            return IO.readAll(in);
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        }
    }
}
