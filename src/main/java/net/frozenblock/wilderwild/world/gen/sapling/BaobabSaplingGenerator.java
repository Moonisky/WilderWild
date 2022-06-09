package net.frozenblock.wilderwild.world.gen.sapling;

import net.frozenblock.wilderwild.world.feature.WildTreeConfigured;
import net.minecraft.block.sapling.LargeTreeSaplingGenerator;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

public class BaobabSaplingGenerator extends LargeTreeSaplingGenerator {

    public BaobabSaplingGenerator() {}


    @Nullable
    @Override
    protected RegistryEntry<? extends ConfiguredFeature<?, ?>> getLargeTreeFeature(Random random) {
        return random.nextFloat() < 0.856F ? WildTreeConfigured.BAOBAB : WildTreeConfigured.BAOBAB_TALL;
    }

    @Nullable
    @Override
    protected RegistryEntry<? extends ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        return null;
    }
}
