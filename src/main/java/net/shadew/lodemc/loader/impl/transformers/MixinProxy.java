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

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.reflect.Constructor;

import net.shadew.lodemc.loader.api.transformer.Transformer;

public class MixinProxy implements Transformer {
    private static Class<?> cls;
    private final IMixinTransformer transformer = newTransformer();

    static {
        try {
            cls = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static IMixinTransformer newTransformer() {
        try {
            Constructor<?> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (IMixinTransformer) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canTransform(String className) {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String className, byte[] bytes) {
        return transformer.transformClassBytes(className, className, bytes);
    }
}
