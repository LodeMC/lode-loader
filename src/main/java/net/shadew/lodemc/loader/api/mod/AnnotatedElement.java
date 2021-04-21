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

package net.shadew.lodemc.loader.api.mod;

import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.Map;

public interface AnnotatedElement {
    String getPathToClass();
    Mod getMod();

    Type getContainerType();
    String getMemberName();
    Type getMemberDescriptor();
    int getMemberModifiers();

    Type getAnnotationType();
    Map<String, Object> getAnnotationValues();

    ElementType getElementType();
}
