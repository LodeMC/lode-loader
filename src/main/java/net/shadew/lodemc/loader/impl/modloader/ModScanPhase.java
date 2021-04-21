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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.impl.LoaderPhase;
import net.shadew.lodemc.loader.impl.PhaseStatus;

public class ModScanPhase implements LoaderPhase<Void> {
    private final Map<String, List<LoaderException>> problems = new LinkedHashMap<>();
    private PhaseStatus status = PhaseStatus.SUCCESS;

    private final List<ModImpl> mods;

    public ModScanPhase(List<ModImpl> mods) {
        this.mods = mods;
    }

    @Override
    public String name() {
        return "ScanMods";
    }

    @Override
    public Void result() {
        return null;
    }

    @Override
    public PhaseStatus status() {
        return status;
    }

    @Override
    public Map<String, List<LoaderException>> problems() {
        return problems;
    }

    @Override
    public void run(LodeLoader loader) {
        for (ModImpl mod : mods) {
            Path root = mod.getRootPath();
            try (Stream<Path> allPaths = Files.walk(root)) {
                allPaths.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .forEach(path -> process(root.relativize(path).toString().replace('\\', '/'), load(path), mod));
            } catch (IOException exc) {
                handleProblem(mod.getId(), new LoaderException(exc).critical().problem(mod));
            } catch (LoaderException exc) {
                handleProblem(mod.getId(), exc.critical().problem(mod));
            }
        }
    }

    private void handleProblem(String type, LoaderException problem) {
        problems.computeIfAbsent(type, k -> new ArrayList<>()).add(problem);

        PhaseStatus pendingStatus = problem.isCritical() ? PhaseStatus.FATAL : PhaseStatus.WARNING;
        if (pendingStatus.compareTo(status) > 0) {
            status = pendingStatus;
        }
    }

    private ClassNode load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            return node;
        } catch (IOException exc) {
            throw new LoaderException(exc);
        }
    }

    private void process(String path, ClassNode cls, ModImpl mod) {
        if (cls.visibleAnnotations != null) {
            for (AnnotationNode ann : cls.visibleAnnotations) {
                mod.addAnnotatedElement(AnnotatedElementImpl.forClass(path, mod, cls, ann));
            }
        }
        if (cls.invisibleAnnotations != null) {
            for (AnnotationNode ann : cls.invisibleAnnotations) {
                mod.addAnnotatedElement(AnnotatedElementImpl.forClass(path, mod, cls, ann));
            }
        }

        for (MethodNode method : cls.methods) {
            if (method.visibleAnnotations != null) {
                for (AnnotationNode ann : method.visibleAnnotations) {
                    mod.addAnnotatedElement(AnnotatedElementImpl.forMethod(path, mod, cls, method, ann));
                }
            }
            if (method.invisibleAnnotations != null) {
                for (AnnotationNode ann : method.invisibleAnnotations) {
                    mod.addAnnotatedElement(AnnotatedElementImpl.forMethod(path, mod, cls, method, ann));
                }
            }
        }

        for (FieldNode field : cls.fields) {
            if (field.visibleAnnotations != null) {
                for (AnnotationNode ann : field.visibleAnnotations) {
                    mod.addAnnotatedElement(AnnotatedElementImpl.forField(path, mod, cls, field, ann));
                }
            }
            if (field.invisibleAnnotations != null) {
                for (AnnotationNode ann : field.invisibleAnnotations) {
                    mod.addAnnotatedElement(AnnotatedElementImpl.forField(path, mod, cls, field, ann));
                }
            }
        }
    }
}
