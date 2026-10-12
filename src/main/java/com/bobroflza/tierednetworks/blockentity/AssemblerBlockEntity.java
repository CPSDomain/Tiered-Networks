package com.bobroflza.tierednetworks.blockentity;

import com.bobroflza.tierednetworks.crafting.Pattern;
import com.bobroflza.tierednetworks.registry.ModBlockEntities;
import com.bobroflza.tierednetworks.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Crafts one crafting-table recipe at a time for a Pattern Provider it touches,
 * then sends the result (and leftovers like empty buckets) into the provider's network.
 */
public class AssemblerBlockEntity extends BlockEntity {
    public static final int CRAFT_TICKS = 10;

    private final List<ItemStack> grid = new ArrayList<>();
    private final List<ItemStack> outputs = new ArrayList<>();
    @Nullable private UUID network;
    private int progress;

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLER.get(), pos, state);
    }

    public boolean isBusy() {
        return !grid.isEmpty() || !outputs.isEmpty();
    }

    public boolean isWorking() {
        return !grid.isEmpty();
    }

    public void start(List<ItemStack> craftingGrid, UUID networkId) {
        grid.clear();
        grid.addAll(craftingGrid);
        network = networkId;
        progress = 0;
        setChanged();
        updateLit();
    }

    private void updateLit() {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(com.bobroflza.tierednetworks.block.AssemblerBlock.LIT)
                && state.getValue(com.bobroflza.tierednetworks.block.AssemblerBlock.LIT) != isWorking()) {
            level.setBlock(worldPosition, state.setValue(com.bobroflza.tierednetworks.block.AssemblerBlock.LIT, isWorking()), 3);
        }
    }

    public void serverTick() {
        if (!grid.isEmpty() && ++progress >= CRAFT_TICKS) {
            craft();
            updateLit();
        }
        if (!outputs.isEmpty() && network != null) {
            MinecraftServer server = level.getServer();
            if (server == null) return;
            List<ItemStack> still = new ArrayList<>();
            for (ItemStack s : outputs) {
                ItemStack left = NetworkStorage.insert(server, network, s);
                if (!left.isEmpty()) still.add(left);
            }
            if (still.size() != outputs.size() || !still.equals(outputs)) setChanged();
            outputs.clear();
            outputs.addAll(still);
        }
    }

    private void craft() {
        CraftingContainer container = Pattern.container(grid);
        Optional<CraftingRecipe> recipe = Pattern.findRecipe(level, container);
        if (recipe.isPresent()) {
            outputs.add(recipe.get().assemble(container, level.registryAccess()));
            for (ItemStack rest : recipe.get().getRemainingItems(container)) {
                if (!rest.isEmpty()) outputs.add(rest);
            }
        } else {
            // The recipe is gone (datapack change): give the ingredients back.
            for (ItemStack s : grid) if (!s.isEmpty()) outputs.add(s);
        }
        grid.clear();
        progress = 0;
        setChanged();
    }

    /** Called when the block is broken: nothing is lost. */
    public void dropContents() {
        for (ItemStack s : grid) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        for (ItemStack s : outputs) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
        grid.clear();
        outputs.clear();
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        grid.clear();
        outputs.clear();
        readList(tag.getList("Grid", Tag.TAG_COMPOUND), grid);
        readList(tag.getList("Outputs", Tag.TAG_COMPOUND), outputs);
        network = tag.hasUUID("Network") ? tag.getUUID("Network") : null;
        progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Grid", writeList(grid));
        tag.put("Outputs", writeList(outputs));
        if (network != null) tag.putUUID("Network", network);
        tag.putInt("Progress", progress);
    }

    private static ListTag writeList(List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack s : stacks) list.add(s.save(new CompoundTag()));
        return list;
    }

    private static void readList(ListTag list, List<ItemStack> into) {
        for (int i = 0; i < list.size(); i++) into.add(ItemStack.of(list.getCompound(i)));
    }
}
