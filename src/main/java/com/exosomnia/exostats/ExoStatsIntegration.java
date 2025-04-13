package com.exosomnia.exostats;

import it.crystalnest.harvest_with_ease.api.event.HarvestEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ExoStatsIntegration {

    @SubscribeEvent
    public void harvestEaseEvent(final HarvestEvents.AfterHarvestEvent event)
    {
        BlockState state = event.getCrop();
        Block block = state.getBlock();
        ServerPlayer player = event.getEntity();
        ExoStats.BreakScoreEntry breakScore = ExoStats.breakScoreMap.get(block);

        if (breakScore != null) {
            Integer score = breakScore.calculatedScore().apply(player, state);
            if (score != 0) { player.awardStat(breakScore.scoreLocation(), score); }
        }
    }
}
