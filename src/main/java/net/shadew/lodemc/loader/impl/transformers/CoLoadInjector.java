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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.shadew.lodemc.loader.api.mod.LoadWith;
import net.shadew.lodemc.loader.api.transformer.Transformer;
import net.shadew.lodemc.loader.impl.ASMUtil;
import net.shadew.lodemc.loader.impl.modloader.AnnotatedElementImpl;

public class CoLoadInjector implements Transformer {
    public static final Type CO_LOAD_ANNOTATION = Type.getType(LoadWith.class);
    private static final Map<String, List<Type>> CO_LOAD_MAP = new TreeMap<>();

    public static void registerCoLoad(String target, Type type) {
        CO_LOAD_MAP.computeIfAbsent(target, k -> new ArrayList<>()).add(type);
    }

    @Override
    public boolean canTransform(String className) {
        return CO_LOAD_MAP.containsKey(className);
    }

    @Override
    public byte[] transformClassBytes(String className, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        transform(node);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private void transform(ClassNode node) {
        List<Type> coLoad = CO_LOAD_MAP.get(node.name);
        if (coLoad == null) return;

        MethodNode clinit = ASMUtil.findMethod(node, "<clinit>", "()V")
                                   .orElseGet(() -> {
                                       MethodNode meth = new MethodNode();
                                       meth.instructions.add(new InsnNode(Opcodes.RETURN));
                                       node.methods.add(meth);
                                       return meth;
                                   });

        ASMUtil.findInsnBefore(clinit.instructions.getLast(), Opcodes.RETURN).ifPresent(insn -> {
            coLoad.forEach(type -> ASMUtil.insertBefore(
                clinit.instructions, insn,
                new LdcInsnNode(type),
                new MethodInsnNode(Opcodes.INVOKESTATIC, "net/shadew/lodemc/loader/hooks/LodeHooks", "loadClass", "(Ljava/lang/Class;)V")
            ));
        });
    }

    @SuppressWarnings("unchecked")
    public static void process(AnnotatedElementImpl elm) {
        if (elm.getAnnotationType().equals(CO_LOAD_ANNOTATION) && elm.getElementType() == ElementType.TYPE) {
            List<Type> value = (List<Type>) elm.getAnnotationValues().get("value");

            for (Type type : value)
                registerCoLoad(type.getInternalName(), elm.getContainerType());
        }
    }
}
