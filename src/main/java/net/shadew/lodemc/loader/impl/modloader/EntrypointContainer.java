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

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.shadew.lodemc.loader.api.Environment;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.mod.AnnotatedElement;
import net.shadew.lodemc.loader.api.mod.Entrypoint;

public class EntrypointContainer {
    public static final Type ENTRYPOINT_ANNOTATION = Type.getType(Entrypoint.class);

    private final String[] requiredMods;
    private final Environment env;
    private final AnnotatedElement element;

    public EntrypointContainer(String[] requiredMods, Environment env, AnnotatedElement element) {
        this.requiredMods = requiredMods;
        this.env = env;
        this.element = element;
    }

    public boolean isSafeToLoad(LodeLoader loader) {
        if (element.getElementType() != ElementType.METHOD && element.getElementType() != ElementType.TYPE)
            return false;

        for (String mod : requiredMods) {
            if (!loader.isModLoaded(mod)) {
                return false;
            }
        }

        if (env == null) {
            return true;
        }

        return env == loader.getEnvironment();
    }

    public void call(LodeLoader loader) {
        if (element.getElementType() == ElementType.METHOD) {
            String name = element.getMemberName();
            Type desc = element.getMemberDescriptor();
            int mods = element.getMemberModifiers();
            if (desc.getArgumentTypes().length > 0) {
                throw new RuntimeException("Cannot call @Entrypoint method '" + name + "' as it has arguments");
            }
            if ((mods & Opcodes.ACC_STATIC) == 0) {
                throw new RuntimeException("Cannot call @Entrypoint method '" + name + "' as it is not static");
            }
            if ((mods & Opcodes.ACC_PUBLIC) == 0) {
                throw new RuntimeException("Cannot call @Entrypoint method '" + name + "' as it is not public");
            }

            Method method = findMethod(element.getContainerType(), name, desc, loader.getGameClassLoader());
            if (method == null) return;

            try {
                method.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException exc) {
                throw new RuntimeException("Failed to call @Entrypoint method '" + name + "'", exc);
            }
        }
        if (element.getElementType() == ElementType.TYPE) {
            try {
                Class.forName(element.getContainerType().getClassName(), true, loader.getGameClassLoader());
            } catch (ClassNotFoundException ignored) {
                // A class should exist if it is annotated - only reason we reach here is that it was stripped by
                // a later transformer
            }
        }
    }

    private Class<?> findClass(Type owner, ClassLoader clsLoader) {
        if (owner.getSort() == Type.METHOD) return null;
        if (owner.getSort() == Type.BYTE) return byte.class;
        if (owner.getSort() == Type.SHORT) return short.class;
        if (owner.getSort() == Type.INT) return int.class;
        if (owner.getSort() == Type.LONG) return long.class;
        if (owner.getSort() == Type.FLOAT) return float.class;
        if (owner.getSort() == Type.DOUBLE) return double.class;
        if (owner.getSort() == Type.CHAR) return char.class;
        if (owner.getSort() == Type.BOOLEAN) return boolean.class;
        if (owner.getSort() == Type.ARRAY) {
            String clsname = owner.getInternalName().replace('/', '.');
            try {
                return Class.forName(clsname);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        String clsname = owner.getClassName();
        try {
            return clsLoader.loadClass(clsname);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Method findMethod(Type owner, String name, Type desc, ClassLoader loader) {
        Class<?> ownerCls = findClass(owner, loader);
        if (ownerCls == null) return null;

        Type[] args = desc.getArgumentTypes();
        Class<?>[] argTypes = new Class<?>[args.length];

        for (int i = 0, l = args.length; i < l; i++) {
            Class<?> cls = findClass(args[i], loader);
            if (cls == null) return null;
            argTypes[i] = cls;
        }

        try {
            return ownerCls.getMethod(name, argTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static EntrypointContainer tryInstantiate(AnnotatedElement element) {
        if (element.getAnnotationType().equals(ENTRYPOINT_ANNOTATION)) {
            Map<String, Object> values = element.getAnnotationValues();
            String[] requires;

            if (values.containsKey("requires")) {
                List<?> list = (List<?>) values.get("requires");
                requires = new String[list.size()];
                int i = 0;
                for (Object o : list) {
                    requires[i++] = (String) o;
                }
            } else {
                requires = new String[0];
            }

            boolean client = false, server = false;
            if (values.containsKey("env")) {
                List<?> list = (List<?>) values.get("env");

                for (Object o : list) {
                    String[] ev = (String[]) o;
                    if (ev[1].equals(Environment.CLIENT.name())) {
                        client = true;
                    }
                    if (ev[1].equals(Environment.SERVER.name())) {
                        server = true;
                    }
                }
            } else {
                client = true;
                server = true;
            }


            Environment env = null;
            if (client && !server) env = Environment.CLIENT;
            if (!client && server) env = Environment.SERVER;

            // In case they really want this @Entrypoint to load in neither client or server env, just ignore it
            if (!client && !server) return null;

            return new EntrypointContainer(requires, env, element);
        }
        return null;
    }
}
