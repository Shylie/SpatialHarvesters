package dunkmania101.spatialharvesters.objects.blocks;

import dunkmania101.spatialharvesters.init.TileEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LootHarvesterBlock extends HarvesterBlock {
    public LootHarvesterBlock(Properties properties, int tierIn) {
        super(properties, tierIn);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityInit.LOOT_HARVESTER.get().create(pos, state);
    }
}