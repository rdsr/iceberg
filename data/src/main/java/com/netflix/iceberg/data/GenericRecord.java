/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.iceberg.data;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.netflix.iceberg.StructLike;
import com.netflix.iceberg.types.Types;
import com.netflix.iceberg.types.Types.StructType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GenericRecord implements Record, StructLike {
  private static final LoadingCache<StructType, Map<String, Integer>> NAME_MAP_CACHE =
      CacheBuilder.newBuilder()
      .weakKeys()
      .build(new CacheLoader<StructType, Map<String, Integer>>() {
        @Override
        public Map<String, Integer> load(StructType struct) {
          Map<String, Integer> idToPos = Maps.newHashMap();
          List<Types.NestedField> fields = struct.fields();
          for (int i = 0; i < fields.size(); i += 1) {
            idToPos.put(fields.get(i).name(), i);
          }
          return idToPos;
        }
      });

  public static GenericRecord create(StructType struct) {
    return new GenericRecord(struct);
  }

  private final StructType struct;
  private final Object[] values;
  private final Map<String, Integer> nameToPos;

  private GenericRecord(StructType struct) {
    this.struct = struct;
    this.values = new Object[struct.fields().size()];
    this.nameToPos = NAME_MAP_CACHE.getUnchecked(struct);
  }

  @Override
  public StructType struct() {
    return struct;
  }

  @Override
  public Object getField(String name) {
    Integer pos = nameToPos.get(name);
    if (pos != null) {
      return values[pos];
    }

    return null;
  }

  @Override
  public void setField(String name, Object value) {
    Integer pos = nameToPos.get(name);
    Preconditions.checkArgument(pos != null, "Cannot set unknown field named: " + name);
    values[pos] = value;
  }

  @Override
  public Object get(int pos) {
    return values[pos];
  }

  @Override
  public <T> T get(int pos, Class<T> javaClass) {
    Object value = get(pos);
    if (javaClass.isInstance(value)) {
      return javaClass.cast(value);
    } else {
      throw new IllegalStateException("Not an instance of " + javaClass.getName() + ": " + value);
    }
  }

  @Override
  public <T> void set(int pos, T value) {
    values[pos] = value;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    GenericRecord that = (GenericRecord) other;
    return Arrays.deepEquals(this.values, that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(values);
  }
}
