package com.example.killaura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.Comparator;

public class KillAuraClient implements ClientModInitializer {
    private static boolean enabled = false;
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // 注册 /km on 和 /km off 命令
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("km")
                .then(ClientCommandManager.literal("on").executes(context -> {
                    enabled = true;
                    context.getSource().sendFeedback(Text.literal("§a[KillAura] 已开启 (重锤 20CPS 模式)"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(context -> {
                    enabled = false;
                    context.getSource().sendFeedback(Text.literal("§c[KillAura] 已关闭"));
                    return 1;
                }))
            );
        });

        // 客户端 Tick 事件，每游戏 Tick 触发一次 (20 TPS = 20 CPS)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;
            if (client.player == null || client.world == null || client.interactionManager == null) return;

            // 检查主手或副手是否拿着重锤 (Mace)
            boolean holdingMace = client.player.getMainHandStack().isOf(Items.MACE) ||
                                  client.player.getOffHandStack().isOf(Items.MACE);
            if (!holdingMace) return;

            // 控制 20 CPS 频率 (每个 Tick 攻击一次)
            tickCounter++;
            if (tickCounter < 1) {
                return;
            }
            tickCounter = 0;

            // 寻找攻击范围内的目标 (4.5 格半径)
            double reach = 4.5;
            Box box = client.player.getBoundingBox().expand(reach);
            
            Entity target = client.world.getOtherEntities(client.player, box, entity -> 
                entity instanceof LivingEntity && 
                entity.isAlive() && 
                entity != client.player && 
                !entity.isSpectator()
            ).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);

            if (target != null) {
                // 强制无视蓄力：重置冷却进度
                client.player.resetLastAttackedTicks();
                
                // 攻击目标
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }
}
