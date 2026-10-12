package com.bobroflza.tierednetworks.item;

import com.bobroflza.tierednetworks.blockentity.ControllerBlockEntity;
import com.bobroflza.tierednetworks.blockentity.CraftingBlockEntity;
import com.bobroflza.tierednetworks.crafting.CraftingManager;
import com.bobroflza.tierednetworks.blockentity.NetworkNodeBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.grid.NodeKind;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Sneak + right-click a controller to copy its network.
 * Right-click any other network block to link it to that network.
 * Sneak + right-click any linked network block to copy its network too (owner or op).
 * Sneak + right-click air to clear the linker.
 */
public class NetworkLinkerItem extends Item {
    private static final String TAG_NETWORK = "LinkedNetwork";

    public NetworkLinkerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Nullable
    public static UUID getLinkedNetwork(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_NETWORK) ? tag.getUUID(TAG_NETWORK) : null;
    }

    /**
     * The network a player may copy from this device: they must own it (or be an op).
     * Tells the player why not and returns null otherwise. Server side only.
     */
    @Nullable
    public static UUID copyNetwork(Player player, NetworkNodeBlockEntity node) {
        UUID id = node.getNetworkId();
        if (id == null || player.level().getServer() == null) return null;
        ControllerBlockEntity controller = node instanceof ControllerBlockEntity c ? c
                : NetworkTracker.getController(player.level().getServer(), id);
        boolean allowed = player.hasPermissions(2) || (controller != null && controller.isOwner(player));
        if (!allowed) {
            player.displayClientMessage(Component.translatable(controller == null
                    ? "message.tierednetworks.copy_needs_controller" : "message.tierednetworks.not_owner")
                    .withStyle(ChatFormatting.RED), true);
            return null;
        }
        return id;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof NetworkNodeBlockEntity node)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        if (node instanceof ControllerBlockEntity controller && !player.isShiftKeyDown()) {
            player.displayClientMessage(controller.describeController(), false);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            if (node.getNetworkId() == null) {
                player.displayClientMessage(node.describeNetwork(), false);
                return InteractionResult.CONSUME;
            }
            UUID copied = copyNetwork(player, node);
            if (copied == null) return InteractionResult.FAIL;
            stack.getOrCreateTag().putUUID(TAG_NETWORK, copied);
            player.displayClientMessage(Component.translatable("message.tierednetworks.linker_bound", copied.toString().substring(0, 8))
                    .withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        UUID id = getLinkedNetwork(stack);
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.tierednetworks.linker_empty").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        if (node instanceof CraftingBlockEntity cpuBlock) {
            // Linking one block of a Crafting CPU links the whole CPU.
            CraftingManager.Cpu cpu = CraftingManager.cpuOf(cpuBlock);
            for (CraftingBlockEntity b : cpu.blocks) b.setNetworkId(id);
            player.displayClientMessage(Component.translatable("message.tierednetworks.cpu_linked", cpu.blocks.size(),
                    id.toString().substring(0, 8), node.getStatus().display()).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
        node.setNetworkId(id);
        if (node.getNodeKind() == NodeKind.ACCESS_POINT) {
            // Access points switch on at the controller's next mesh update, up to a second later.
            player.displayClientMessage(Component.translatable("message.tierednetworks.ap_linked", id.toString().substring(0, 8))
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tierednetworks.device_linked",
                    id.toString().substring(0, 8), node.getStatus().display()).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getLinkedNetwork(stack) != null) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    tag.remove(TAG_NETWORK);
                    if (tag.isEmpty()) stack.setTag(null);
                }
                player.displayClientMessage(Component.translatable("message.tierednetworks.linker_cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLinkedNetwork(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        UUID id = getLinkedNetwork(stack);
        if (id == null) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.linker_unbound").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.tierednetworks.linker_bound", id.toString().substring(0, 8)).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.tierednetworks.linker_help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
