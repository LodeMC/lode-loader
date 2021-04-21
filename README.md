# Lode

Lode is a tiny mod loader independent from Fabric, Quilt or Forge, aiming to make the modding environment as simple as
possible.

This project is heavily in development. It is currently not possible to install Lode, and it is neither possible to set
up a development environment for Lode.

## Warning!

This project is dependent on Minecraft itself. The EULA of Mojang prohibits any public distributions of Minecraft, and
hence the only legal Maven repository to get Minecraft from is your local maven repository (found at `%USER_HOME%\.m2`
or `~/.m2`). You will have to download and remap Minecraft yourself and deploy it in your local Maven repository in
order to build this project. Minecraft must be remapped using Mojang mappings.

## Design

These are the most important Design features of Lode, some are implemented, some aren't.

- The environment uses Mojang's official mappings in both the IDE and the launcher. This has various benefits:
    - Integrating dependent mods in your IDE is as easy as installing them into the launcher. No extra remapping
      required.
    - Logs and crash reports show names that are easy to understand, whether they come from the launcher environment or
      the development environment.
    - No need to deal with different mappings when using transformers or reflection, nor the need of refmaps when using
      Mixin.
- The Lode loader has an open API and can be easily extended using plugins:
    - Mods can define plugins as part of their mod. These plugins are then loaded by the loader before any mod itself is
      loaded.
    - Plugins can define class sources and transformers as preferred.
- Lode integrates easily with the Mixin framework, allowing for mods to quickly inject code into the Minecraft codebase
  without the need for defining plugins and registering (often quite complicated) custom class transformers.
- Mods are being scanned for annotations when they are loaded, allowing for API mods to easily find classes or members
  with a certain annotation. To speed things up, Minecraft is not scanned (since it will never have any needed
  annotation).
- The Lode loader is as simple as possible, it only provides the necessary API for loading mods. A more extended
  compatibility API will be provided as a separate mod, and will be completely optional.
- Mods will be able to include dependent mods within their JAR files, so that there is no need to add dependency mods
  manually.
- Lode will be completely open source, so that anyone is able to contribute to the project.

## Fabric/Quilt support

Currently, Lode cannot load mods from other mod loaders. Lode isn't going to support Forge mods for the time being, but
support for Fabric/Quilt mods is planned.

Support for other mod loaders has been the initial reason for providing the plugin API. This way, mods can define custom
loading techniques, creating the possibility to load mods from other mod loaders, or allowing the use of other
languages.

Currently, Lode will only be able to load Java mods specifically developed for Lode.

## License

Copyright 2021 Shadew

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

# Samples

Here are a few tiny code features of Lode.

### Annotation-based entrypoints

```java
@Entrypoint
public static void init() {
    // This method is called on initialization
}
```

```java
@Entrypoint
public class LoadMe {
    // This class is initialized on initialization
}
```

### Load classes with other classes

```java
@LoadWith(Blocks.class)
public class ILoadWithBlocks {
    // This class is initialized as soon as Blocks
    // is initialized
}
```

### Client/Server exclusion

```java
@ClientOnly
public void iExistOnTheClientOnly() {
}

@ServerOnly
public void iExistOnTheDedicatedServerOnly() {
}
```

### Plugins

`lode_loader_plugin.json`

```json
{
  "id": "example",
  "package": "path.to.plugin.package",
  "plugins": [
    "SomePluginClass"
  ]
}
```

```java
public class SomePluginClass implements LoaderPlugin {
    @Override
    public void init(LodeLoader loader) {
        // Initialized by the loader before mods are loaded
        // You can register custom transformers here
    }
}
```

### Mixin

```java
@Mixin(net.minecraft.client.main.Main.class)
public class MainMixin {
    @Inject(method = "main", at = @At("HEAD"))
    private static void onMain(String[] args, CallbackInfo info) {
        // Invoked as soon as the Minecraft client is started
        System.out.println("MIXIN IS APPLIED");
    }
}
```
