package net.frozenblock.wilderwild.block;

import net.frozenblock.wilderwild.block.entity.StoneChestBlockEntity;
import net.frozenblock.wilderwild.registry.RegisterBlockEntities;
import net.frozenblock.wilderwild.registry.RegisterSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class StoneChestBlock extends ChestBlock {
    //public static final BooleanProperty ANCIENT = RegisterProperties.ANCIENT;

    public StoneChestBlock(Properties settings, Supplier<BlockEntityType<? extends ChestBlockEntity>> supplier) {
        super(settings, supplier);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false));

    }

    @Override
    public InteractionResult use(net.minecraft.world.level.block.state.BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StoneChestBlockEntity stoneChest) {
            if (stoneChest.closing) {
                return InteractionResult.FAIL;
            }
            StoneChestBlockEntity stoneEntity = stoneChest.getLeftEntity(world, pos, state, stoneChest);
            if (canInteract(world, pos)) {
                MenuProvider namedScreenHandlerFactory = this.getMenuProvider(state, world, pos);
                if (!hasLid(world, pos) && (!player.isShiftKeyDown() || stoneEntity.openProgress >= 0.5F) && namedScreenHandlerFactory != null) {
                    player.openMenu(namedScreenHandlerFactory);
                    player.awardStat(this.getOpenChestStat());
                    PiglinAi.angerNearbyPiglins(player, true);
                } else if (stoneEntity.openProgress < 0.5F) {
                    MenuProvider lidCheck = (MenuProvider) ((Optional) this.getBlockEntitySourceIgnoreLid(state, world, pos, false).apply(STONE_NAME_RETRIEVER)).orElse(null);
                    boolean first = stoneEntity.openProgress == 0F;
                    if (lidCheck == null) {
                        if (stoneEntity.openProgress < 0.05F) {
                            stoneEntity.openProgress = !player.isCreative() ? stoneEntity.openProgress + 0.025F : 0.05F;
                        } else {
                            return InteractionResult.PASS;
                        }
                    } else {
                        stoneEntity.openProgress = !player.isCreative() ? stoneEntity.openProgress + 0.025F : stoneEntity.openProgress + 0.05F;
                    }
                    stoneEntity.stillLidTicks = (int) (Math.max((stoneEntity.openProgress), 0.2) * 120);
                    StoneChestBlockEntity.playSound(world, pos, state, first ? RegisterSounds.BLOCK_STONE_CHEST_OPEN : RegisterSounds.BLOCK_STONE_CHEST_LIFT, 0.5F);
                    world.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
                    stoneEntity.updateSync();
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    public static boolean hasLid(Level world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StoneChestBlockEntity stoneChest) {
            return stoneChest.openProgress < 0.3F;
        }
        return false;
    }

    public static boolean canInteract(Level world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StoneChestBlockEntity stoneChest) {
            return !(stoneChest.closing || stoneChest.cooldownTicks > 0);
        }
        return true;
    }

    public static boolean hasLid(LevelAccessor world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StoneChestBlockEntity stoneChest) {
            return stoneChest.openProgress < 0.3F;
        }
        return false;
    }

    public static boolean canInteract(LevelAccessor world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof StoneChestBlockEntity stoneChest) {
            return !(stoneChest.closing || stoneChest.cooldownTicks > 0);
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new StoneChestBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, net.minecraft.world.level.block.state.BlockState state, BlockEntityType<T> type) {
        return world.isClientSide ? BaseEntityBlock.createTickerHelper(type, RegisterBlockEntities.STONE_CHEST, StoneChestBlockEntity::clientStoneTick) : BaseEntityBlock.createTickerHelper(type, RegisterBlockEntities.STONE_CHEST, StoneChestBlockEntity::serverStoneTick);
    }

    @Nullable
    public MenuProvider getMenuProvider(net.minecraft.world.level.block.state.BlockState state, Level world, BlockPos pos) {
        return (MenuProvider) ((Optional) this.combine(state, world, pos, false).apply(STONE_NAME_RETRIEVER)).orElse(null);
    }

    @Override
    public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(net.minecraft.world.level.block.state.BlockState state, Level world2, BlockPos pos2, boolean ignoreBlocked) {
        BiPredicate<LevelAccessor, BlockPos> biPredicate = ignoreBlocked ? (world, pos) -> false : StoneChestBlock::isStoneChestBlocked;
        return DoubleBlockCombiner.combineWithNeigbour((BlockEntityType) this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, state, world2, pos2, biPredicate);
    }

    public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> getBlockEntitySourceIgnoreLid(net.minecraft.world.level.block.state.BlockState state, Level world2, BlockPos pos2, boolean ignoreBlocked) {
        BiPredicate<LevelAccessor, BlockPos> biPredicate = ignoreBlocked ? (world, pos) -> false : StoneChestBlock::isStoneChestBlockedNoLid;
        return DoubleBlockCombiner.combineWithNeigbour((BlockEntityType) this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, state, world2, pos2, biPredicate);
    }

    public static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> STONE_NAME_RETRIEVER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {

        @Override
        public Optional<MenuProvider> acceptDouble(final ChestBlockEntity chestBlockEntity, final ChestBlockEntity chestBlockEntity2) {
            final CompoundContainer inventory = new CompoundContainer(chestBlockEntity, chestBlockEntity2);
            return Optional.of(new MenuProvider() {

                @Override
                @Nullable
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    if (chestBlockEntity.canOpen(playerEntity) && chestBlockEntity2.canOpen(playerEntity)) {
                        chestBlockEntity.unpackLootTable(playerInventory.player);
                        chestBlockEntity2.unpackLootTable(playerInventory.player);
                        return ChestMenu.sixRows(i, playerInventory, inventory);
                    }
                    return null;
                }

                @Override
                public Component getDisplayName() {
                    if (chestBlockEntity.hasCustomName()) {
                        return chestBlockEntity.getDisplayName();
                    }
                    if (chestBlockEntity2.hasCustomName()) {
                        return chestBlockEntity2.getDisplayName();
                    }
                    return Component.translatable("container.double_stone_chest");
                }
            });
        }

        @Override
        public Optional<MenuProvider> acceptSingle(ChestBlockEntity chestBlockEntity) {
            return Optional.of(chestBlockEntity);
        }

        @Override
        public Optional<MenuProvider> acceptNone() {
            return Optional.empty();
        }

    };


    public static boolean isStoneChestBlocked(LevelAccessor world, BlockPos pos) {
        if (hasLid(world, pos)) {
            return true;
        }
        return isBlockedChestByBlock(world, pos) || isCatSittingOnChest(world, pos) || !canInteract(world, pos);
    }

    public static boolean isStoneChestBlockedNoLid(LevelAccessor world, BlockPos pos) {
        return isBlockedChestByBlock(world, pos) || isCatSittingOnChest(world, pos) || !canInteract(world, pos);
    }

    private static boolean isBlockedChestByBlock(BlockGetter world, BlockPos pos) {
        BlockPos blockPos = pos.above();
        return world.getBlockState(blockPos).isRedstoneConductor(world, blockPos);
    }

    private static boolean isCatSittingOnChest(LevelAccessor world, BlockPos pos) {
        List<Cat> list = world.getEntitiesOfClass(Cat.class, new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1));
        if (!list.isEmpty()) {
            for (Cat catEntity : list) {
                if (!catEntity.isInSittingPose()) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState updateShape(net.minecraft.world.level.block.state.BlockState state, Direction direction, net.minecraft.world.level.block.state.BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        if (neighborState.is(this) && direction.getAxis().isHorizontal()) {
            //if (neighborState.get(ANCIENT) == state.get(ANCIENT)) {
                ChestType chestType = neighborState.getValue(TYPE);
                if (state.getValue(TYPE) == ChestType.SINGLE && chestType != ChestType.SINGLE && state.getValue(FACING) == neighborState.getValue(FACING) && ChestBlock.getConnectedDirection(neighborState) == direction.getOpposite()) {
                    return state.setValue(TYPE, chestType.getOpposite());
                }
            //}
        } else if (ChestBlock.getConnectedDirection(state) == direction) {
            return state.setValue(TYPE, ChestType.SINGLE);
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction direction3;
        ChestType chestType = ChestType.SINGLE;
        Direction direction = ctx.getHorizontalDirection().getOpposite();
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        boolean bl = ctx.isSecondaryUseActive();
        Direction direction2 = ctx.getClickedFace();
        if (direction2.getAxis().isHorizontal() && bl && (direction3 = this.candidatePartnerFacing(ctx, direction2.getOpposite())) != null && direction3.getAxis() != direction2.getAxis()) {
            direction = direction3;
            ChestType chestType2 = chestType = direction.getCounterClockWise() == direction2.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
        }
        if (chestType == ChestType.SINGLE && !bl) {
            if (direction == this.candidatePartnerFacing(ctx, direction.getClockWise())) {
                chestType = ChestType.LEFT;
            } else if (direction == this.candidatePartnerFacing(ctx, direction.getCounterClockWise())) {
                chestType = ChestType.RIGHT;
            }
        }
        return this.defaultBlockState().setValue(FACING, direction).setValue(TYPE, chestType).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Nullable
    private Direction candidatePartnerFacing(BlockPlaceContext ctx, Direction dir) {
        net.minecraft.world.level.block.state.BlockState blockState = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(dir));
        return blockState.is(this) && blockState.getValue(TYPE) == ChestType.SINGLE ? blockState.getValue(FACING) : null;
    }

    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state, Level world, BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean moved) {
        if (state.is(newState.getBlock())) {
            return;
        }
        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StoneChestBlockEntity stoneChestBlock) {
                stoneChestBlock.unpackLootTable(null);
                ArrayList<ItemStack> ancientItems = stoneChestBlock.ancientItems();
                if (!ancientItems.isEmpty()) {
                    world.playSound(null, pos, RegisterSounds.BLOCK_STONE_CHEST_ITEM_CRUMBLE, SoundSource.BLOCKS, 0.4F, 0.9F + (world.random.nextFloat() / 10F));
                    for (ItemStack taunt : ancientItems) {
                        spawnBreakParticles(world, taunt, pos);
                    }
                }
                for (ItemStack item : stoneChestBlock.nonAncientItems()) {
                    double d = EntityType.ITEM.getWidth();
                    double e = 1.0 - d;
                    double f = d / 2.0;
                    double g = Math.floor(pos.getX()) + world.random.nextDouble() * e + f;
                    double h = Math.floor(pos.getY()) + world.random.nextDouble() * e;
                    double i = Math.floor(pos.getZ()) + world.random.nextDouble() * e + f;
                    while (!item.isEmpty()) {
                        ItemEntity itemEntity = new ItemEntity(world, g, h, i, item.split(world.random.nextInt(21) + 10));
                        itemEntity.setDeltaMovement(world.random.triangle(0.0, 0.11485000171139836), world.random.triangle(0.2, 0.11485000171139836), world.random.triangle(0.0, 0.11485000171139836));
                        world.addFreshEntity(itemEntity);
                    }
                }
                world.updateNeighbourForOutputSignal(pos, this);
            }
        }
        if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
            world.removeBlockEntity(pos);
        }
    }

    public static void spawnBreakParticles(Level world, ItemStack stack, BlockPos pos) {
        if (world instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, world.random.nextIntBetweenInclusive(0, 3), 0.21875F, 0.21875F, 0.21875F, 0.05D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(FACING, TYPE, WATERLOGGED);
    }


}
