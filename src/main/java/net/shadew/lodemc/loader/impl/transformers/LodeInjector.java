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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.shadew.lodemc.loader.api.transformer.Transformer;
import net.shadew.lodemc.loader.impl.ASMUtil;

public class LodeInjector implements Transformer {
    @Override
    public boolean canTransform(String className) {
        return className.equals("net/minecraft/client/main/Main")
                   || className.equals("net/minecraft/client/ClientBrandRetriever")
                   || className.equals("net/minecraft/server/Bootstrap");
    }

    @Override
    public byte[] transformClassBytes(String className, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        transform(node);
        ClassWriter writer = new ClassWriter(reader, 0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private void transform(ClassNode node) {
        if (node.name.equals("net/minecraft/client/main/Main")) {
            ASMUtil.findMethod(node, "main", "([Ljava/lang/String;)V").ifPresent(method -> {
                ASMUtil.prepend(
                    method.instructions,
                    new VarInsnNode(Opcodes.ALOAD, 0),
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "net/shadew/lodemc/loader/hooks/LodeClientHooks", "launch", "([Ljava/lang/String;)V")
                );

                // INVOKESPECIAL net/minecraft/client/Minecraft.<init> (Lnet/minecraft/client/main/GameConfig;)V
                ASMUtil.findInsnBefore(
                    method.instructions.getLast(),
                    Opcodes.INVOKESPECIAL, "net/minecraft/client/Minecraft", "<init>", "(Lnet/minecraft/client/main/GameConfig;)V"
                ).ifPresent(target -> ASMUtil.insertAfter(
                    method.instructions, target,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "net/shadew/lodemc/loader/hooks/LodeClientHooks", "init", "()V")
                ));
            });
        } else if (node.name.equals("net/minecraft/client/ClientBrandRetriever")) {
            ASMUtil.findMethod(node, "getClientModName", "()Ljava/lang/String;").ifPresent(method -> {
                method.instructions.clear();
                ASMUtil.append(
                    method.instructions,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, "net/shadew/lodemc/loader/hooks/LodeClientHooks", "getClientBrand", "()Ljava/lang/String;"),
                    new InsnNode(Opcodes.ARETURN)
                );
            });
        }
//        else if (node.name.equals("net/minecraft/server/Bootstrap")) {
//            node.methods.stream()
//                        .filter(met -> met.name.equals("bootStrap") && met.desc.equals("()V"))
//                        .findAny()
//                        .ifPresent(main -> {
//                            AbstractInsnNode initInsn = null;
//                            for (AbstractInsnNode insn : main.instructions) {
//                                if (initInsn != null) {
//                                    initInsn = insn; // Found "isBootstrapped = true", now get next instruction to insert before
//                                    break;           // and then break the loop
//                                }
//                                if (insn.getOpcode() == Opcodes.PUTSTATIC) {
//                                    FieldInsnNode fInsn = (FieldInsnNode) insn;
//                                    if (fInsn.owner.equals("net/minecraft/server/Bootstrap")
//                                            && fInsn.name.equals("isBootstrapped")
//                                            && fInsn.desc.equals("Z")) {
//                                        initInsn = fInsn;
//                                    }
//                                }
//                            }
//                            main.instructions.insertBefore(initInsn, new MethodInsnNode(
//                                Opcodes.INVOKESTATIC,
//                                "net/shadew/lodemc/loader/hooks/LodeHooks",
//                                "init", "()V"
//                            ));
//                        });
//        }
    }
}
