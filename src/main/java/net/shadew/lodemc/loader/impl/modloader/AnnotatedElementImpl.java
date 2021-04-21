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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.shadew.lodemc.loader.api.mod.AnnotatedElement;
import net.shadew.lodemc.loader.api.mod.Mod;

public class AnnotatedElementImpl implements AnnotatedElement {
    private final ElementType type;
    private final String pathToClass;
    private final Mod mod;
    private final Type container;
    private final String memberName;
    private final Type memberDesc;
    private final int memberMods;
    private final Type annotation;
    private final Map<String, Object> values;

    private AnnotatedElementImpl(ElementType type, String pathToClass, Mod mod, Type container, String memberName, Type memberDesc, int memberMods, Type annotation, Map<String, Object> values) {
        this.type = type;
        this.pathToClass = pathToClass;
        this.mod = mod;
        this.container = container;
        this.memberName = memberName;
        this.memberDesc = memberDesc;
        this.memberMods = memberMods;
        this.annotation = annotation;
        this.values = Collections.unmodifiableMap(values);
    }

    @Override
    public String getPathToClass() {
        return pathToClass;
    }

    @Override
    public Mod getMod() {
        return mod;
    }

    @Override
    public Type getContainerType() {
        return container;
    }

    @Override
    public String getMemberName() {
        return memberName;
    }

    @Override
    public Type getMemberDescriptor() {
        return memberDesc;
    }

    @Override
    public int getMemberModifiers() {
        return memberMods;
    }

    @Override
    public Type getAnnotationType() {
        return annotation;
    }

    @Override
    public Map<String, Object> getAnnotationValues() {
        return values;
    }

    @Override
    public ElementType getElementType() {
        return type;
    }

    private static Map<String, Object> makeValuesMap(AnnotationNode annotation) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (annotation.values != null) {
            for (int i = 0, l = annotation.values.size(); i < l; i += 2) {
                Object val = annotation.values.get(i + 1);
                if (val instanceof List)
                    val = Collections.unmodifiableList((List<?>) val);

                values.put(
                    (String) annotation.values.get(i),
                    val
                );
            }
        }
        return values;
    }

    public static AnnotatedElementImpl forClass(String path, Mod mod, ClassNode node, AnnotationNode annotation) {
        Type classDesc = Type.getObjectType(node.name);
        return new AnnotatedElementImpl(
            (node.access & Opcodes.ACC_ANNOTATION) != 0 ? ElementType.ANNOTATION_TYPE : ElementType.TYPE,
            path, mod,
            classDesc,
            node.name,
            classDesc,
            node.access,
            Type.getType(annotation.desc),
            makeValuesMap(annotation)
        );
    }

    public static AnnotatedElementImpl forMethod(String path, Mod mod, ClassNode owner, MethodNode method, AnnotationNode annotation) {
        return new AnnotatedElementImpl(
            method.name.equals("<init>") ? ElementType.CONSTRUCTOR : ElementType.METHOD,
            path, mod,
            Type.getObjectType(owner.name),
            method.name,
            Type.getMethodType(method.desc),
            method.access,
            Type.getType(annotation.desc),
            makeValuesMap(annotation)
        );
    }

    public static AnnotatedElementImpl forField(String path, Mod mod, ClassNode owner, FieldNode field, AnnotationNode annotation) {
        return new AnnotatedElementImpl(
            ElementType.FIELD, path, mod,
            Type.getObjectType(owner.name),
            field.name,
            Type.getType(field.desc),
            field.access,
            Type.getType(annotation.desc),
            makeValuesMap(annotation)
        );
    }
}
