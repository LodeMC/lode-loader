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

package net.shadew.lodemc.loader.impl.transformers;

import java.io.*;

import net.shadew.lodemc.loader.api.transformer.Transformer;

public class DebugTransformer implements Transformer {
    private final String[] filter;

    public DebugTransformer(String filter) {
        this.filter = filter.split(",");
    }

    @Override
    public boolean canTransform(String className) {
        for (String f : filter)
            if (className.replace('/', '.').startsWith(f)) return true;
        return false;
    }

    @Override
    public byte[] transformClassBytes(String className, byte[] bytes) {
        if (canTransform(className)) {
            File file = new File(".lode/debug/classes/" + className.replace('.', '/') + ".class");
            file.getParentFile().mkdirs();
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                out.write(bytes);
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
        return bytes;
    }
}
