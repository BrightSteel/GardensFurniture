package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.bedrock.BedrockBlock;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record BedrockBlockState(String name, List<Permutation> permutations) {

    @AllArgsConstructor
    public static class Permutation {
        private final String value;
        private final @Nullable String condition;
        private final @Nullable BedrockBlock bedrockBlock;

        public Permutation(String value, @Nullable BedrockBlock bedrockBlock) {
            this.value = value;
            this.condition = null;
            this.bedrockBlock = bedrockBlock;
        }

        public Permutation(String value) {
            this(value, null);
        }

        public String getCondition(String stateName) {
            return condition != null ? condition : "q.block_property('%s') == '%s'".formatted(stateName, value);
        }
    }
}
