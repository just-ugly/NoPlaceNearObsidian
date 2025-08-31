package NoPlaceNearPortal;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class NoPlaceNearPortalMod implements ClientModInitializer {
    private static boolean isEnabled = true;
    private KeyBinding toggleBinding;

    private boolean isPortalSide(BlockPos placementPos, BlockPos portalPos, BlockState portalState) {
        // 计算相对位置
        int dx = placementPos.getX() - portalPos.getX();
        int dy = placementPos.getY() - portalPos.getY();
        int dz = placementPos.getZ() - portalPos.getZ();

        // 检查是否在传送门的侧边
        // 上下方向总是被阻止
        if (Math.abs(dy) == 1 && dx == 0 && dz == 0) {
            return true;
        }

        // 检查水平方向
        // 如果是X轴传送门（东西向），阻止南北方向
        // 如果是Z轴传送门（南北向），阻止东西方向
        if (dy == 0) {

            return Arrays.equals(portalState.get(NetherPortalBlock.AXIS).getDirections(), Direction.Axis.X.getDirections()) ?
                    Math.abs(dx) == 1 && dz == 0 : Math.abs(dz) == 1 && dx == 0;

        }

        return false;
    }

    @Override
    public void onInitializeClient() {
        // 注册快捷键
        toggleBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.noplacenearportal.toggle",
            InputUtil.Type.KEYSYM,
            -1, // 默认使用V键
            "category.noplacenearportal.general"
        ));

        // 注册快捷键检查事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleBinding.wasPressed()) {
                isEnabled = !isEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable(isEnabled ? "noplacenearportal.switch.enabled" : "noplacenearportal.switch.disabled"), true);
                }
            }
        });

        // 注册方块使用事件
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!isEnabled) return ActionResult.PASS;

            // 检查是否正在尝试放置方块
            if (player.getStackInHand(hand).getItem() instanceof BlockItem) {
                BlockPos placementPos = hitResult.getBlockPos().offset(hitResult.getSide());

                // 检查周围的方块
                BlockPos[] checkPositions = {
                    placementPos.north(),
                    placementPos.south(),
                    placementPos.east(),
                    placementPos.west(),
                    placementPos.up(),
                    placementPos.down()
                };

                // 检查每个位置是否有传送门方块
                for (BlockPos checkPos : checkPositions) {
                    BlockState checkState = world.getBlockState(checkPos);
                    if (checkState.getBlock() == Blocks.NETHER_PORTAL) {
                        if (isPortalSide(placementPos, checkPos, checkState)) {
                            if (player.getWorld().isClient) {
                                player.sendMessage(Text.translatable("noplacenearportal.message"), true);
                            }
                            return ActionResult.FAIL;
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });
    }
}
