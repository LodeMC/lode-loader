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

package net.shadew.lodemc.loader.bootstrap;

import net.shadew.lodemc.loader.api.Environment;
import net.shadew.lodemc.loader.impl.LodeLoaderImpl;
import net.shadew.lodemc.loader.impl.classloader.ExtendableClassLoader;

public class LoaderBootstrap {
    public static void main(String[] args) {
        ExtendableClassLoader classLoader = new ExtendableClassLoader(LoaderBootstrap.class.getClassLoader());
        LodeLoaderImpl.start(Environment.CLIENT, classLoader);
        LodeLoaderImpl.instance.launch(
            "net.minecraft.client.main.Main",
            "--version", "1.16.4",
            "--accessToken", "69",
            "--assetsDir", "C:\\Users\\Shadew\\.gradle\\caches\\fabric-loom\\assets",
            "--assetIndex", "1.16.4-1.16"
        );
    }
}
