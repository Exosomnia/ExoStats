package com.exosomnia.exostats;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ExoStats.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static String defaultBreakScores;
    static {
        defaultBreakScores = "#forge:stone,10;" +
                "#minecraft:coal_ores,320;" +
                "#minecraft:copper_ores,240;" +
                "#minecraft:iron_ores,640;" +
                "#minecraft:gold_ores,1280;" +
                "#minecraft:redstone_ores,640;" +
                "#minecraft:lapis_ores,1280;" +
                "#minecraft:emerald_ores,2560;" +
                "#minecraft:diamond_ores,2560;" +
                "#forge:ores/quartz,320;" +
                "#forge:ores/niter,320;" +
                "#forge:ores/tin,640;" +
                "#forge:ores/silver,1280;" +
                "#forge:ores/nickel,640;" +
                "#forge:ores/zinc,320;" +
                "minecraft:nether_gold_ore,640;" +
                "majruszsdifficulty:enderium_shard_ore,20480;" +
                "#minecraft:crops,25;" +
                "minecraft:nether_wart,320;" +
                "minecraft:cocoa,240;";
    }


    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue VILLAGER_TRADE_SCORE = BUILDER
            .comment("Score awarded when trading with villagers")
            .defineInRange("villagerTradeScore", 200, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue ITEMS_TO_EMERALDS_MULTIPLIER = BUILDER
            .comment("Score multiplier with trading items to emeralds with villagers.")
            .defineInRange("itemsToEmeraldsMultiplier", 3.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENCHANTED_FISHING_BONUS = BUILDER
            .comment("Fishing score bonus when fishing enchanted items")
            .defineInRange("enchantedFishingBonus", 400, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<String> BREAK_SCORES = BUILDER
            .comment("scores awarded when breaking certain blocks")
            .define("breakScores", defaultBreakScores);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int villagerTradeScore;
    public static double itemsToEmeraldsMultiplier;
    public static int enchantedFishingBonus;
    public static ImmutableMap<String, Integer> breakScores;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        villagerTradeScore = VILLAGER_TRADE_SCORE.get();
        itemsToEmeraldsMultiplier = ITEMS_TO_EMERALDS_MULTIPLIER.get();
        enchantedFishingBonus = ENCHANTED_FISHING_BONUS.get();

        //Parse mining scores
        ImmutableMap.Builder<String,Integer> breakScoreBuilder = new ImmutableMap.Builder<>();
        String breakScoresString = BREAK_SCORES.get();
        for (String scoreEntry : breakScoresString.split(";")) {
            String[] entryValues = scoreEntry.split(",");
            if (entryValues.length <= 1) continue;
            breakScoreBuilder.put(entryValues[0], Integer.valueOf(entryValues[1]));
        }
        breakScores = breakScoreBuilder.build();
    }
}
