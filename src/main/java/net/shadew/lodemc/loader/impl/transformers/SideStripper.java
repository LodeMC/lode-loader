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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.shadew.lodemc.loader.api.Environment;
import net.shadew.lodemc.loader.api.transformer.Transformer;
import net.shadew.lodemc.sidemarker.ClientOnly;
import net.shadew.lodemc.sidemarker.ExclusiveInterfaces;
import net.shadew.lodemc.sidemarker.ServerOnly;

public class SideStripper implements Transformer {
    private static final Type CLIENT_ONLY = Type.getType(ClientOnly.class);
    private static final Type SERVER_ONLY = Type.getType(ServerOnly.class);
    private static final Type EXCLUSIVE_INTERFACES = Type.getType(ExclusiveInterfaces.class);

    private final Environment environment;

    public SideStripper(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean canTransform(String className) {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String className, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        node = transform(node);
        if (node == null) return null;
        ClassWriter writer = new ClassWriter(reader, 0);
        node.accept(writer);
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private ClassNode transform(ClassNode node) {
        Type annotationType = environment == Environment.CLIENT ? SERVER_ONLY : CLIENT_ONLY;
        if (getAnnotation(node.invisibleAnnotations, annotationType) != null) {
            return null;
        }
        if (getAnnotation(node.visibleAnnotations, annotationType) != null) {
            return null;
        }

        node.fields.removeIf(
            n -> getAnnotation(n.invisibleAnnotations, annotationType) != null
                     || getAnnotation(n.visibleAnnotations, annotationType) != null
        );
        node.methods.removeIf(
            n -> getAnnotation(n.invisibleAnnotations, annotationType) != null
                     || getAnnotation(n.visibleAnnotations, annotationType) != null
        );

        AnnotationNode ifcAnnotation = getAnnotation(node.invisibleAnnotations, EXCLUSIVE_INTERFACES);
        if (ifcAnnotation == null)
            ifcAnnotation = getAnnotation(node.visibleAnnotations, EXCLUSIVE_INTERFACES);

        if (ifcAnnotation != null) {
            if (ifcAnnotation.values != null) {
                Set<Type> excludeInterfaces = new HashSet<>();
                for (int i = 0, l = ifcAnnotation.values.size(); i < l; i += 2) {
                    String key = (String) ifcAnnotation.values.get(i);
                    Object val = ifcAnnotation.values.get(i + 1);

                    if (key.equals(environment == Environment.CLIENT ? "serverOnly" : "clientOnly")) {
                        excludeInterfaces.addAll((List<Type>) val);
                    }
                }

                node.interfaces.removeIf(ifc -> excludeInterfaces.contains(Type.getObjectType(ifc)));
            }
        }

        return node;
    }

    private static AnnotationNode getAnnotation(List<AnnotationNode> annotations, Type type) {
        if (annotations == null) return null;
        for (AnnotationNode node : annotations) {
            if (node.desc.equals(type.getDescriptor())) {
                return node;
            }
        }
        return null;
    }
}
