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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!enabled) return;
            if (client.player == null || client.world == null || client.interactionManager == null) return;

            boolean holdingMace = client.player.getMainHandStack().isOf(Items.MACE) ||
                                  client.player.getOffHandStack().isOf(Items.MACE);
            if (!holdingMace) return;

            tickCounter++;
            if (tickCounter < 1) {
                return;
            }
            tickCounter = 0;

            double reach = 4.5;
            Box box = client.player.getBoundingBox().expand(reach);
            
            Entity target = client.world.getOtherEntities(client.player, box, entity -> 
                entity instanceof LivingEntity && 
                entity.isAlive() && 
                entity != client.player && 
                !entity.isSpectator()
            ).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);

            if (target != null) {
                client.player.resetLastAttackedTicks();
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        });
    }
}
