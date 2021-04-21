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

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import net.shadew.lodemc.loader.api.Environment;
import net.shadew.lodemc.loader.api.LoaderException;
import net.shadew.lodemc.loader.api.LodeLoader;
import net.shadew.lodemc.loader.api.mod.Mod;
import net.shadew.lodemc.loader.api.mod.ModConfig;
import net.shadew.lodemc.loader.api.plugin.LoaderPlugin;
import net.shadew.lodemc.loader.api.transformer.ClassPath;
import net.shadew.lodemc.loader.api.transformer.Transformer;
import net.shadew.lodemc.loader.impl.classloader.ExtendableClassLoader;
import net.shadew.lodemc.loader.impl.classloader.GameClassLoader;
import net.shadew.lodemc.loader.impl.classloader.MainClassPath;
import net.shadew.lodemc.loader.impl.discovery.ModDiscoveryPhase;
import net.shadew.lodemc.loader.impl.discovery.ModFile;
import net.shadew.lodemc.loader.impl.modloader.ModImpl;
import net.shadew.lodemc.loader.impl.modloader.ModLoaderPhase;
import net.shadew.lodemc.loader.impl.modloader.ModScanPhase;
import net.shadew.lodemc.loader.impl.pluginloader.PluginInfo;
import net.shadew.lodemc.loader.impl.pluginloader.PluginInitPhase;
import net.shadew.lodemc.loader.impl.pluginloader.PluginInstantiatorPhase;
import net.shadew.lodemc.loader.impl.pluginloader.PluginLoaderPhase;
import net.shadew.lodemc.loader.impl.transformers.*;

public class LodeLoaderImpl implements LodeLoader {
    public static LodeLoaderImpl instance;

    private final Environment environment;
    private final ExtendableClassLoader agentClassLoader;
    private final GameClassLoader gameClassLoader;
    private final List<ModFile> modFiles = new ArrayList<>();
    private final List<PluginInfo> pluginInfo = new ArrayList<>();
    private final List<ModImpl> mods = new ArrayList<>();
    private final List<Mod> modsImm = Collections.unmodifiableList(mods);
    private final Map<String, Mod> modsById = new LinkedHashMap<>();
    private final List<ClassPath> defaultClassPaths = new ArrayList<>();
    private final Map<String, List<LoaderPlugin>> plugins = new LinkedHashMap<>();

    private final List<Path> modsFolders = new ArrayList<>();
    private LoaderPhase<?> currentPhase;

    private boolean transformersFrozen;

    private LodeLoaderImpl(Environment environment, ExtendableClassLoader agentClassLoader) {
        this.environment = environment;
        this.agentClassLoader = agentClassLoader;
        this.gameClassLoader = new GameClassLoader(agentClassLoader);
    }

    public void start() {
        addModsFolder(Paths.get("mods"));

        // Find all mod jars, directories, or whateer
        modFiles.addAll(startPhase(ModDiscoveryPhase.INSTANCE));
        System.out.println("Found " + modFiles.size() + " mod files");

        // Add all mod jars to the agent class loader, so we can load plugin classes from them
        modFiles.forEach(agentClassLoader::addClassPath);

        // Load plugins (these are loaded from the classpath)
        pluginInfo.addAll(startPhase(PluginLoaderPhase.INSTANCE));

        // Add agent packages: these are the packages defined in lode_loader_plugin.json files
        pluginInfo.forEach(info -> gameClassLoader.addAgentPackage(info.getPluginPackage()));

        // Instantiate plugins
        plugins.putAll(startPhase(new PluginInstantiatorPhase(pluginInfo)));
        System.out.println("Loaded " + plugins.size() + " plugins");

        // Initialize mod loading
        initDefaults();
        startPhase(new PluginInitPhase(plugins, LoaderPlugin::init));
        transformersFrozen = true;

        // Load mods
        mods.addAll(startPhase(new ModLoaderPhase(modFiles)));
        mods.forEach(mod -> modsById.put(mod.getId(), mod));
        loadMixinConfigs();

        // Scan mods for annotated elements
        startPhase(new ModScanPhase(mods));

        // Setup game class loader
        defaultClassPaths.forEach(gameClassLoader::addClassPath);
        mods.forEach(mod -> gameClassLoader.addClassPath(mod.getClassPath()));

        // Post-initialize
        startPhase(new PluginInitPhase(plugins, LoaderPlugin::postInit));

        Thread.currentThread().setContextClassLoader(gameClassLoader);
        System.out.println("Loaded " + mods.size() + " mods");
    }

    private void initDefaults() {
        installClassPath(new MainClassPath(getClass().getClassLoader()));
        MixinBootstrap.init();
        setMixinToDefaultPhase();
        installTransformer(new SideStripper(getEnvironment()));
        installTransformer(new CoLoadInjector());
        installTransformer(new LodeInjector());
        installPostTransformer(new MixinProxy());
        if (System.getProperties().containsKey("lode.debug.output_classes")) {
            System.out.println("Outputting classes to .lode/debug/classes");
            installPostTransformer(new DebugTransformer(System.getProperty("lode.debug.output_classes")));
        }
    }

    private void loadMixinConfigs() {
        mods.stream()
            .map(Mod::getConfig)
            .map(ModConfig::getMixinConfigs)
            .flatMap(
                mixins -> mixins.entrySet()
                                .stream()
                                .filter(e -> e.getKey().equals("minecraft") || isModLoaded(e.getKey()))
                                .flatMap(e -> e.getValue().stream())
            )
            .forEach(Mixins::addConfiguration);
    }

    private static void setMixinToDefaultPhase() {
        try {
            Method m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            m.setAccessible(true);
            m.invoke(null, MixinEnvironment.Phase.INIT);
            m.invoke(null, MixinEnvironment.Phase.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void launch(String mainClass, String... args) {
        try {
            gameClassLoader.loadClass(mainClass)
                           .getMethod("main", String[].class)
                           .invoke(null, (Object) args);
        } catch (Exception exc) {
            throw new LoaderException("Failed to launch", exc).critical();
        }
    }

    public void addModsFolder(Path modsFolder) {
        modsFolders.add(modsFolder);
    }

    public LoaderPhase<?> getCurrentPhase() {
        return currentPhase;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public List<Path> getModsFolders() {
        return modsFolders;
    }

    public ExtendableClassLoader getAgentClassLoader() {
        return agentClassLoader;
    }

    @Override
    public GameClassLoader getGameClassLoader() {
        return gameClassLoader;
    }

    @Override
    public List<Mod> getMods() {
        return modsImm;
    }

    public List<ModImpl> getNonAPIMods() {
        return mods;
    }

    @Override
    public Mod getModById(String id) {
        return modsById.get(id);
    }

    @Override
    public boolean isModLoaded(String id) {
        return modsById.containsKey(id);
    }

    public List<ClassPath> getDefaultClassPaths() {
        return defaultClassPaths;
    }

    @Override
    public void installClassPath(ClassPath classPath) {
        if (transformersFrozen) {
            throw new RuntimeException("Plugin initialization is over");
        }
        defaultClassPaths.add(classPath);
    }

    @Override
    public void installTransformer(Transformer transformer) {
        if (transformersFrozen) {
            throw new RuntimeException("Plugin initialization is over");
        }
        gameClassLoader.addTransformer(transformer);
    }

    private void installPostTransformer(Transformer transformer) {
        gameClassLoader.addPostTransformer(transformer);
    }

    public <T> T startPhase(LoaderPhase<T> phase) {
        currentPhase = phase;

        phase.run(this);

        if (phase.status() != PhaseStatus.SUCCESS) {
            System.err.println("Phase " + phase.name() + " finished with status " + phase.status());
            printProblems(phase.problems());
            if (phase.status() == PhaseStatus.FATAL) {
                throw new LoaderException("Phase " + phase.name() + " failed! Can't continue loading!").critical();
            }
        }

        currentPhase = null;
        return phase.result();
    }

    private void printProblems(Map<String, List<LoaderException>> problems) {
        problems.forEach((type, excs) -> {
            System.err.println("-- " + type + " --");
            excs.forEach(Throwable::printStackTrace);
        });
    }

    public static void start(Environment environment, ExtendableClassLoader agentClassLoader) {
        instance = new LodeLoaderImpl(environment, agentClassLoader);
        instance.start();
    }
}
