package com.electric.gbyte;

import com.electric.gbyte.internal.$GByte$Preconditions;
import com.electric.gbyte.internal.bind.TypeAdapters;
import com.electric.gbyte.reflect.TypeToken;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bingo
 */
@NoArgsConstructor
public final class GByteBuilder {

    private final Map<Type, InstanceCreator<?>> instanceCreators = new HashMap<>();

    private final List<TypeAdapterFactory> factories = new ArrayList<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public GByteBuilder registerTypeAdapter(Type type, Object typeAdapter) {
        $GByte$Preconditions.checkArgument(typeAdapter instanceof InstanceCreator<?> || typeAdapter instanceof TypeAdapter<?>);
        if (typeAdapter instanceof InstanceCreator<?>) {
            instanceCreators.put(type, (InstanceCreator) typeAdapter);
        }
        if (typeAdapter instanceof TypeAdapter<?>) {
            factories.add(TypeAdapters.newFactory(TypeToken.get(type), (TypeAdapter) typeAdapter));
        }
        return this;
    }

    public GByte create() {
        return new GByte(instanceCreators, this.factories);
    }

}
