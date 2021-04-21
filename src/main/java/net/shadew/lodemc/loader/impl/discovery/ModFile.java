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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.transformer.ClassPath;

public class ModFile implements ClassPath {
    private final FileSystem fs;
    private final Path filePath;
    private final Path root;
    private final ModFile owner;
    private ModFile container;

    public ModFile(Path path, ModFile owner) {
        this.owner = owner;

        filePath = path;
        if (Files.isDirectory(path)) {
            fs = path.getFileSystem();
            root = path;
        } else {
            try {
                fs = FileSystems.newFileSystem(path, null);
                root = fs.getPath("/");
            } catch (Exception exc) {
                throw new LoaderException("Exception instantiating ModFile", exc).critical().problem(this);
            }
        }

        container = owner;
        if (container != null)
            while (container.owner != null) {
                container = container.owner;
            }
        else
            container = this;
    }

    public ModFile getContainer() {
        return container;
    }

    public ModFile getOwner() {
        return owner;
    }

    public FileSystem getFileSystem() {
        return fs;
    }

    public Path getFilePath() {
        return filePath;
    }

    public Path findFile(String filePath) {
        return root.resolve(filePath);
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return "ModFile [ filePath: " + filePath + ", container: " + container + "]";
    }

    @Override
    public Iterable<URL> findSources(String path) {
        class Itr implements Iterator<URL> {
            private URL url;

            Itr(URL url) {
                this.url = url;
            }

            @Override
            public boolean hasNext() {
                return url != null;
            }

            @Override
            public URL next() {
                if (url == null) throw new NoSuchElementException();
                URL out = url;
                url = null;
                return out;
            }
        }

        URL url = null;
        Path file = findFile(path);
        if (Files.exists(file) && Files.isRegularFile(file)) {
            try {
                url = file.toUri().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        URL finalUrl = url;
        return () -> new Itr(finalUrl);
    }
}
