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

package net.shadew.lodemc.loader.impl.classloader;

import java.util.HashSet;
import java.util.Set;

public class GameClassLoader extends ExtendableClassLoader {
    private final ClassLoader agent;
    private final Set<String> agentPackages = new HashSet<>();

    public GameClassLoader(ClassLoader agent) {
        super(null);
        this.agent = agent;
    }

    public void addAgentPackage(String pkg) {
        agentPackages.add(pkg);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (String pkg : agentPackages) {
            if (name.startsWith(pkg + ".")) {
                return agent.loadClass(name);
            }
        }
        return super.findClass(name);
    }
}
