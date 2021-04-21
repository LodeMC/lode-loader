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

package net.shadew.lodemc.loader.impl.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

import net.shadew.lodemc.loader.impl.LodeLoaderImpl;

public class LodeClassProvider implements IClassProvider, IClassBytecodeProvider {
    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, LodeLoaderImpl.instance.getGameClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LodeLoaderImpl.instance.getGameClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LodeLoaderImpl.instance.getAgentClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException {
        ClassReader reader = new ClassReader(LodeLoaderImpl.instance.getGameClassLoader().getClassBytes(name));
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException {
        return getClassNode(name);
    }
}
