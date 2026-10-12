package com.bobroflza.tierednetworks.item;

import com.bobroflza.tierednetworks.blockentity.NetworkNodeBlockEntity;
import com.bobroflza.tierednetworks.grid.NetworkStatus;
import com.bobroflza.tierednetworks.grid.NetworkTracker;
import com.bobroflza.tierednetworks.menu.TerminalHost;
import com.bobroflza.tierednetworks.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A Network Terminal you carry. Sneak + right-click a controller or any linked device to bind it,
 * then right-click anywhere the network reaches - the same range rules as any device.
 */
public class WirelessTerminalItem extends Item {
    private static final String TAG_NETWORK = "LinkedNetwork";

    public WirelessTerminalItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Nullable
    public static UUID getNetwork(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TAG_NETWORK) ? tag.getUUID(TAG_NETWORK) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof NetworkNodeBlockEntity node)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (node.getNetworkId() == null) {
            player.displayClientMessage(node.describeNetwork(), true);
            return InteractionResult.FAIL;
        }
        UUID id = NetworkLinkerItem.copyNetwork(player, node);
        if (id == null) return InteractionResult.FAIL;
        ctx.getItemInHand().getOrCreateTag().putUUID(TAG_NETWORK, id);
        player.displayClientMessage(Component.translatable("message.tierednetworks.wireless_bound", id.toString().substring(0, 8))
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        UUID id = getNetwork(stack);
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.tierednetworks.wireless_unbound").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        Host host = new Host(sp, hand, id);
        NetworkStatus status = host.hostStatus();
        if (status != NetworkStatus.ONLINE) {
            player.displayClientMessage(status.display(), true);
            return InteractionResultHolder.fail(stack);
        }
        NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                (containerId, inv, p) -> new TerminalMenu(containerId, inv, host), stack.getHoverName()));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getNetwork(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        UUID id = getNetwork(stack);
        if (id == null) {
            tooltip.add(Component.translatable("tooltip.tierednetworks.linker_unbound").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.tierednetworks.linker_bound", id.toString().substring(0, 8)).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.tierednetworks.wireless_help").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The open terminal follows the player: online while they are in range of the network. */
    private record Host(ServerPlayer player, InteractionHand hand, UUID network) implements TerminalHost {
        @Override
        public UUID hostNetwork() {
            return network;
        }

        @Override
        public NetworkStatus hostStatus() {
            MinecraftServer server = player.getServer();
            if (server == null) return NetworkStatus.UNLINKED;
            return NetworkTracker.status(server, network, GlobalPos.of(player.level().dimension(), player.blockPosition()));
        }

        @Nullable
        @Override
        public MinecraftServer hostServer() {
            return player.getServer();
        }

        @Override
        public boolean hostStillValid(Player p) {
            ItemStack held = p.getItemInHand(hand);
            return held.getItem() instanceof WirelessTerminalItem && network.equals(getNetwork(held));
        }
    }
}
