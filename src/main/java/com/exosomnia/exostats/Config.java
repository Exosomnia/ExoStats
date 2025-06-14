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
                "#minecraft:coal_ores,480;" +
                "#minecraft:copper_ores,360;" +
                "#minecraft:iron_ores,960;" +
                "#minecraft:gold_ores,1920;" +
                "#minecraft:redstone_ores,960;" +
                "#minecraft:lapis_ores,1920;" +
                "#minecraft:emerald_ores,3840;" +
                "#minecraft:diamond_ores,3840;" +
                "#forge:ores/quartz,960;" +
                "#forge:ores/niter,480;" +
                "#forge:ores/tin,960;" +
                "#forge:ores/silver,1920;" +
                "#forge:ores/nickel,960;" +
                "#forge:ores/zinc,480;" +
                "minecraft:nether_gold_ore,1920;" +
                "majruszsdifficulty:enderium_shard_ore,28800;" +
                "#minecraft:crops,40;" +
                "minecraft:nether_wart,320;" +
                "minecraft:cocoa,160;" +
                "minecraft:amethyst_cluster,960;";
    }


    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue VILLAGER_TRADE_SCORE = BUILDER
            .comment("Score awarded when trading with villagers")
            .defineInRange("villagerTradeScore", 200, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue ITEMS_TO_EMERALDS_MULTIPLIER_HUSBANDRY = BUILDER
            .comment("Score multiplier when trading items to emeralds with husbandry villagers.")
            .defineInRange("itemsToEmeraldsMultiplier", 9.6, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue FISH_TO_EMERALDS_MULTIPLIER = BUILDER
            .comment("Score multiplier when trading fish to emeralds with fisher villagers.")
            .defineInRange("fishToEmeraldsMultiplier", 24.0, 0.0, Double.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENCHANTED_FISHING_BONUS = BUILDER
            .comment("Fishing score bonus when fishing treasure items")
            .defineInRange("enchantedFishingBonus", 1600, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue NETHERITE_XP = BUILDER
            .comment("Mining xp when smelting netherite scrap")
            .defineInRange("netheriteXPAmount", 11520, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<String> BREAK_SCORES = BUILDER
            .comment("scores awarded when breaking certain blocks")
            .define("breakScores", defaultBreakScores);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int villagerTradeScore;
    public static double itemsToEmeraldsMultiplierHusbandry;
    public static double fishToEmeraldsMultiplier;
    public static int enchantedFishingBonus;
    public static int netheriteXPAmount;
    public static ImmutableMap<String, Integer> breakScores;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        villagerTradeScore = VILLAGER_TRADE_SCORE.get();
        itemsToEmeraldsMultiplierHusbandry = ITEMS_TO_EMERALDS_MULTIPLIER_HUSBANDRY.get();
        fishToEmeraldsMultiplier = FISH_TO_EMERALDS_MULTIPLIER.get();
        enchantedFishingBonus = ENCHANTED_FISHING_BONUS.get();
        netheriteXPAmount = NETHERITE_XP.get();

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
