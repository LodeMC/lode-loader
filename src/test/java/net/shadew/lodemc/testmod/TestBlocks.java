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

package net.shadew.lodemc.testmod;

import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import net.shadew.lodemc.loader.api.mod.LoadWith;

@LoadWith(Blocks.class)
public class TestBlocks {
    static {
        Block block = new Block(BlockBehaviour.Properties.of(Material.STONE));
        Registry.register(Registry.BLOCK, "test_lode:block", block);

        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            Block.BLOCK_STATE_REGISTRY.add(state);
        }
        block.getLootTable();
    }
}
