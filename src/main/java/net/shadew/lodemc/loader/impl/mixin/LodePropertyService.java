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

package net.shadew.lodemc.loader.impl.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

public class LodePropertyService implements IGlobalPropertyService {
    private final Map<String, Object> props = new HashMap<>();

    static class Key implements IPropertyKey {
        private final String key;

        Key(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }

    public LodePropertyService() {
    }

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key) {
        return (T) props.get(key.toString());
    }

    @Override
    public final void setProperty(IPropertyKey key, Object value) {
        props.put(key.toString(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = props.get(key.toString());
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = props.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }

}
