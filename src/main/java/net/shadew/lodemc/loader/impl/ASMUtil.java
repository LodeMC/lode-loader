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

package net.shadew.lodemc.loader.impl;

import org.objectweb.asm.tree.*;

import java.util.Optional;
import java.util.function.Predicate;

public class ASMUtil {
    public static Optional<MethodNode> findMethod(ClassNode cls, String name, String desc) {
        for (MethodNode method : cls.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    public static Optional<FieldNode> findField(ClassNode cls, String name, String desc) {
        for (FieldNode field : cls.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    public static Optional<FieldNode> findField(ClassNode cls, String name) {
        for (FieldNode field : cls.fields) {
            if (field.name.equals(name)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    public static void prepend(InsnList insns, AbstractInsnNode... newInsns) {
        InsnList newInsnList = new InsnList();
        for (AbstractInsnNode i : newInsns) {
            newInsnList.add(i);
        }
        insns.insert(newInsnList);
    }

    public static void insertBefore(InsnList insns, AbstractInsnNode insn, AbstractInsnNode... newInsns) {
        InsnList newInsnList = new InsnList();
        for (AbstractInsnNode i : newInsns) {
            newInsnList.add(i);
        }
        insns.insertBefore(insn, newInsnList);
    }

    public static void insertAfter(InsnList insns, AbstractInsnNode insn, AbstractInsnNode... newInsns) {
        InsnList newInsnList = new InsnList();
        for (AbstractInsnNode i : newInsns) {
            newInsnList.add(i);
        }
        insns.insert(insn, newInsnList);
    }

    public static void append(InsnList insns, AbstractInsnNode... newInsns) {
        for (AbstractInsnNode i : newInsns) {
            insns.add(i);
        }
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, int opcode) {
        while (insn != null) {
            if (insn.getOpcode() == opcode) {
                return Optional.of(insn);
            }
            insn = insn.getNext();
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, int opcode, String owner, String name, String desc) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fInsn = (FieldInsnNode) insn;
                        if (!fInsn.owner.equals(owner)) continue;
                        if (!fInsn.name.equals(name)) continue;
                        if (!fInsn.desc.equals(desc)) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode mInsn = (MethodInsnNode) insn;
                        if (!mInsn.owner.equals(owner)) continue;
                        if (!mInsn.name.equals(name)) continue;
                        if (!mInsn.desc.equals(desc)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getNext();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, int opcode, String v) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof TypeInsnNode) {
                        TypeInsnNode tInsn = (TypeInsnNode) insn;
                        if (!tInsn.desc.equals(v)) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof LdcInsnNode) {
                        LdcInsnNode lInsn = (LdcInsnNode) insn;
                        if (!lInsn.cst.equals(v)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getNext();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, int opcode, int v) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof VarInsnNode) {
                        VarInsnNode vInsn = (VarInsnNode) insn;
                        if (vInsn.var != v) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof IntInsnNode) {
                        IntInsnNode iInsn = (IntInsnNode) insn;
                        if (iInsn.operand != v) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof LdcInsnNode) {
                        LdcInsnNode lInsn = (LdcInsnNode) insn;
                        if (!lInsn.cst.equals(v)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getNext();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, int opcode, int v, int inc) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof IincInsnNode) {
                        IincInsnNode vInsn = (IincInsnNode) insn;
                        if (vInsn.var != v) continue;
                        if (vInsn.incr != inc) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getNext();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, Predicate<? super AbstractInsnNode> predicate) {
        while (insn != null) {
            if (predicate.test(insn)) {
                return Optional.of(insn);
            }
            insn = insn.getNext();
        }
        return Optional.empty();
    }

    public static <T extends AbstractInsnNode> Optional<AbstractInsnNode> findInsnAfter(AbstractInsnNode insn, Class<? extends T> type, Predicate<? super T> predicate) {
        while (insn != null) {
            if (type.isInstance(insn) && predicate.test(type.cast(insn))) {
                return Optional.of(insn);
            }
            insn = insn.getNext();
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, int opcode) {
        while (insn != null) {
            if (insn.getOpcode() == opcode) {
                return Optional.of(insn);
            }
            insn = insn.getPrevious();
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, int opcode, String owner, String name, String desc) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fInsn = (FieldInsnNode) insn;
                        if (!fInsn.owner.equals(owner)) continue;
                        if (!fInsn.name.equals(name)) continue;
                        if (!fInsn.desc.equals(desc)) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode mInsn = (MethodInsnNode) insn;
                        if (!mInsn.owner.equals(owner)) continue;
                        if (!mInsn.name.equals(name)) continue;
                        if (!mInsn.desc.equals(desc)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getPrevious();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, int opcode, String v) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof TypeInsnNode) {
                        TypeInsnNode tInsn = (TypeInsnNode) insn;
                        if (!tInsn.desc.equals(v)) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof LdcInsnNode) {
                        LdcInsnNode lInsn = (LdcInsnNode) insn;
                        if (!lInsn.cst.equals(v)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getPrevious();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, int opcode, int v) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof VarInsnNode) {
                        VarInsnNode vInsn = (VarInsnNode) insn;
                        if (vInsn.var != v) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof IntInsnNode) {
                        IntInsnNode iInsn = (IntInsnNode) insn;
                        if (iInsn.operand != v) continue;
                        return Optional.of(insn);
                    }
                    if (insn instanceof LdcInsnNode) {
                        LdcInsnNode lInsn = (LdcInsnNode) insn;
                        if (!lInsn.cst.equals(v)) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getPrevious();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, int opcode, int v, int inc) {
        while (insn != null) {
            try {
                if (insn.getOpcode() == opcode) {
                    if (insn instanceof IincInsnNode) {
                        IincInsnNode vInsn = (IincInsnNode) insn;
                        if (vInsn.var != v) continue;
                        if (vInsn.incr != inc) continue;
                        return Optional.of(insn);
                    }
                }
            } finally {
                insn = insn.getPrevious();
            }
        }
        return Optional.empty();
    }

    public static Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, Predicate<? super AbstractInsnNode> predicate) {
        while (insn != null) {
            if (predicate.test(insn)) {
                return Optional.of(insn);
            }
            insn = insn.getPrevious();
        }
        return Optional.empty();
    }

    public static <T extends AbstractInsnNode> Optional<AbstractInsnNode> findInsnBefore(AbstractInsnNode insn, Class<? extends T> type, Predicate<? super T> predicate) {
        while (insn != null) {
            if (type.isInstance(insn) && predicate.test(type.cast(insn))) {
                return Optional.of(insn);
            }
            insn = insn.getPrevious();
        }
        return Optional.empty();
    }
}
