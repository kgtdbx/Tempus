/**
 * Copyright © 2017-2018 Hashmap, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hashmapinc.server.common.data;

public class EnumUtil<T extends java.lang.Enum> {
    private final Class<T> typeParameterClass;

    public EnumUtil(Class<T> typeParameterClass){
        this.typeParameterClass = typeParameterClass;
    }

    public T parse(String value){
        T enumVal = null;
        if (value != null && value.length() != 0) {
            for (T current : typeParameterClass.getEnumConstants())
                if (current.name().equalsIgnoreCase(value)) {
                    enumVal = current;
                    break;
                }
        }
        return enumVal;
    }
}