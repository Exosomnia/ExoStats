package com.exosomnia.exostats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.function.TriFunction;

import java.util.HashMap;
import java.util.Map;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExoStats.MODID)
public class ExoStats
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "exostats";

    //Stats Registry
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS_REGISTER = DeferredRegister.create(Registries.CUSTOM_STAT, MODID);
    public static final RegistryObject<ResourceLocation> XP_PICKUP_STAT = CUSTOM_STATS_REGISTER.register("xp_pickup_stat", () -> ResourceLocation.fromNamespaceAndPath(MODID, "xp_pickup_stat"));
    public static final RegistryObject<ResourceLocation> POTIONS_USED_STAT = CUSTOM_STATS_REGISTER.register("potions_used_stat", () -> ResourceLocation.fromNamespaceAndPath(MODID, "potions_used_stat"));
    public static final RegistryObject<ResourceLocation> ENCHANTED_BOOKS_STAT = CUSTOM_STATS_REGISTER.register("enchanted_books_stat", () -> ResourceLocation.fromNamespaceAndPath(MODID, "enchanted_books_stat"));
    public static final RegistryObject<ResourceLocation> UNIQUE_CRAFTS_STAT = CUSTOM_STATS_REGISTER.register("unique_crafts_stat", () -> ResourceLocation.fromNamespaceAndPath(MODID, "unique_crafts_stat"));
    public static final RegistryObject<ResourceLocation> ANIMALS_TAMED = CUSTOM_STATS_REGISTER.register("animals_tamed", () -> ResourceLocation.fromNamespaceAndPath(MODID, "animals_tamed"));

    public static final RegistryObject<ResourceLocation> MINING_SCORE = CUSTOM_STATS_REGISTER.register("mining_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "mining_score"));
    public static final RegistryObject<ResourceLocation> OCCULT_SCORE = CUSTOM_STATS_REGISTER.register("occult_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "occult_score"));
    public static final RegistryObject<ResourceLocation> EXPLORATION_SCORE = CUSTOM_STATS_REGISTER.register("exploration_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "exploration_score"));
    public static final RegistryObject<ResourceLocation> COMBAT_SCORE = CUSTOM_STATS_REGISTER.register("combat_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "combat_score"));
    public static final RegistryObject<ResourceLocation> FISHING_SCORE = CUSTOM_STATS_REGISTER.register("fishing_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "fishing_score"));
    public static final RegistryObject<ResourceLocation> HUSBANDRY_SCORE = CUSTOM_STATS_REGISTER.register("husbandry_score", () -> ResourceLocation.fromNamespaceAndPath(MODID, "husbandry_score"));

    public static final TagKey<Item> TAG_FISHES = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.fromNamespaceAndPath("forge", "raw_fishes"));
    public static final TagKey<Item> TAG_ORE_SCRAPS = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.fromNamespaceAndPath("exostats", "ore_scraps"));
    public static final TagKey<Item> TAG_FISHING_TREASURE = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), ResourceLocation.fromNamespaceAndPath("exostats", "fishing_treasure_bonus"));

    record BreakScoreEntry(String blockOrTag, ResourceLocation scoreLocation, TriFunction<Player, BlockState, Integer, Integer> scoreFunction) {
        static int score;
    }
    static ImmutableList<BreakScoreEntry> registeredBreakScores;
    static ImmutableMap<Block, BreakScoreEntry> breakScoreMap;

    static ImmutableSet<VillagerProfession> husbandryVillagers;
    static ImmutableSet<VillagerProfession> fishingVillagers;
    static {
        husbandryVillagers = ImmutableSet.of(VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.LEATHERWORKER,
                VillagerProfession.SHEPHERD);
        fishingVillagers = ImmutableSet.of(VillagerProfession.FISHERMAN);
    }

    public ExoStats()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::setupEvent);

        // Register the Deferred Register to the mod event bus so tabs get registered
        CUSTOM_STATS_REGISTER.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void xpEvent(final PlayerXpEvent.PickupXp event)
    {
        event.getEntity().awardStat(Stats.CUSTOM.get(XP_PICKUP_STAT.get()), event.getOrb().getValue());
    }

    @SubscribeEvent
    public void anvilEvent(final AnvilRepairEvent event)
    {
        Player eventPlayer = event.getEntity();
        if (event.getRight().getItem() == Items.ENCHANTED_BOOK) {
            eventPlayer.awardStat(Stats.CUSTOM.get(ENCHANTED_BOOKS_STAT.get()), 1);
        }
    }

    @SubscribeEvent
    public void potionDrinkEvent(final LivingEntityUseItemEvent.Finish event)
    {
        LivingEntity eventEntity = event.getEntity();
        ItemStack eventItem = event.getItem();
        if ((eventEntity instanceof Player) && (eventItem.getItem() == Items.POTION) && (!PotionUtils.getPotion(eventItem).getEffects().isEmpty())) {
                    ((Player)eventEntity).awardStat(Stats.CUSTOM.get(POTIONS_USED_STAT.get()), 1);
        }
    }

    @SubscribeEvent
    public void potionThrowEvent(final EntityJoinLevelEvent event)
    {
        Entity entity = event.getEntity();
        if (entity instanceof ThrownPotion) {
            ThrownPotion potion = (ThrownPotion) entity;
            Entity owner = potion.getOwner();
            if ( (owner instanceof Player player) && (!PotionUtils.getPotion(potion.getItem()).getEffects().isEmpty()) ) {
                player.awardStat(Stats.CUSTOM.get(POTIONS_USED_STAT.get()), 1);
            }
        }
    }

    @SubscribeEvent
    public void itemCraftEvent(final PlayerEvent.ItemCraftedEvent event)
    {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ItemStack craftedItemStack = event.getCrafting();
            int craftedAmount = craftedItemStack.getCount();
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(craftedItemStack.getItem())) == craftedAmount) {
                player.awardStat(UNIQUE_CRAFTS_STAT.get(), 1);
            }
            if (craftedItemStack.is(TAG_ORE_SCRAPS)) {
                player.awardStat(MINING_SCORE.get(), Config.netheriteXPAmount * craftedAmount);
            }
        }
        else {
            LocalPlayer player = (LocalPlayer) event.getEntity();
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(event.getCrafting().getItem())) == event.getCrafting().getCount()) {
                player.awardStat(UNIQUE_CRAFTS_STAT.get(), 1);
            }
        }
    }

    @SubscribeEvent
    public void itemSmeltedEvent(final PlayerEvent.ItemSmeltedEvent event)
    {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ItemStack smeltedItemStack = event.getSmelting();
            int smeltedAmount = smeltedItemStack.getCount();
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(smeltedItemStack.getItem())) == smeltedAmount) {
                player.awardStat(UNIQUE_CRAFTS_STAT.get(), 1);
            }
            if (smeltedItemStack.is(TAG_ORE_SCRAPS)) {
                player.awardStat(MINING_SCORE.get(), Config.netheriteXPAmount * smeltedAmount);
            }
        }
        else {
            LocalPlayer player = (LocalPlayer) event.getEntity();
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(event.getSmelting().getItem())) == event.getSmelting().getCount()) {
                player.awardStat(UNIQUE_CRAFTS_STAT.get(), 1);
            }
        }
    }

    @SubscribeEvent
    public void fishedEvent(final ItemFishedEvent event) {
        Player player = event.getEntity();
        for (ItemStack drop : event.getDrops()) {
            if (drop.isEnchanted() || drop.is(TAG_FISHING_TREASURE)) player.awardStat(FISHING_SCORE.get(), Config.enchantedFishingBonus);
        }
    }

    @SubscribeEvent
    public void tameEvent(final AnimalTameEvent event) {
        event.getTamer().awardStat(Stats.CUSTOM.get(ANIMALS_TAMED.get()), 1);
    }

    @SubscribeEvent
    public void villagerTradeEvent(final TradeWithVillagerEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        AbstractVillager eventVillager = event.getAbstractVillager();
        if (eventVillager instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (husbandryVillagers.contains(profession)) {
                double bonusMod = event.getMerchantOffer().getResult().is(Items.EMERALD) ? Config.itemsToEmeraldsMultiplierHusbandry : 1.0;
                player.awardStat(HUSBANDRY_SCORE.get(), (int)(Config.villagerTradeScore * bonusMod));
            }
            else if (fishingVillagers.contains(profession)) {
                double bonusMod = event.getMerchantOffer().getCostA().is(TAG_FISHES) ? Config.fishToEmeraldsMultiplier : 1.0;
                player.awardStat(FISHING_SCORE.get(), (int)(Config.villagerTradeScore * bonusMod));
            }
        }
    }

    @SubscribeEvent
    public void breakEvent(final BlockEvent.BreakEvent event)
    {
        BlockState state = event.getState();
        Block block = state.getBlock();
        Player player = event.getPlayer();
        BreakScoreEntry breakScore = breakScoreMap.get(block);

        if (breakScore != null) {
            Integer score = breakScore.scoreFunction.apply(player, state, Config.breakScores.get(breakScore.blockOrTag));
            if (score != 0) { player.awardStat(breakScore.scoreLocation, score); }
        }
    }

    @SubscribeEvent
    public void tagUpdateEvent(TagsUpdatedEvent event) {
        Registry<Block> blockRegistry = event.getRegistryAccess().registryOrThrow(Registries.BLOCK);
        Map<Block, BreakScoreEntry> builder = new HashMap<>();
        registeredBreakScores.forEach(entry -> {
            String blockOrTag = entry.blockOrTag;
            if (blockOrTag.startsWith("#")) {
                blockRegistry.getTagOrEmpty(TagKey.create(Registries.BLOCK, ResourceLocation.bySeparator(blockOrTag.substring(1), ':'))).forEach(blockHolder -> {
                    builder.put(blockHolder.get(), entry);
                });
            }
            else {
                Block iterateBlock = blockRegistry.get(ResourceLocation.bySeparator(blockOrTag, ':'));
                if (iterateBlock != null) {
                    builder.put(iterateBlock, entry);
                }
            }
        });
        breakScoreMap = ImmutableMap.copyOf(builder);
    }

    public void setupEvent(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("harvest_with_ease")) {
                MinecraftForge.EVENT_BUS.register(new ExoStatsIntegration());
            }
            registeredBreakScores = ImmutableList.of(
                    new BreakScoreEntry("#forge:stone", MINING_SCORE.get(), (player, blockState, score) -> score),
                    new BreakScoreEntry("#minecraft:coal_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:copper_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:iron_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:gold_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:redstone_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:lapis_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:emerald_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:diamond_ores", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/quartz", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/niter", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/tin", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/silver", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/nickel", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#forge:ores/zinc", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("minecraft:nether_gold_ore", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("majruszsdifficulty:enderium_shard_ore", MINING_SCORE.get(), this::miningScoreOf),
                    new BreakScoreEntry("#minecraft:crops", HUSBANDRY_SCORE.get(), this::husbandryScoreOf),
                    new BreakScoreEntry("minecraft:nether_wart", HUSBANDRY_SCORE.get(), (player, blockState, score) -> blockState.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE ? 0 : Config.breakScores.get("#minecraft:crops") * 4),
                    new BreakScoreEntry("minecraft:cocoa", HUSBANDRY_SCORE.get(), (player, blockState, score) -> blockState.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE ? 0 : Config.breakScores.get("#minecraft:crops") * 4),
                    new BreakScoreEntry("minecraft:amethyst_cluster", OCCULT_SCORE.get(), this::miningScoreOf)
            );
        });
    }

    private int miningScoreOf(Player player, BlockState blockState, Integer originalScore) {
        if (EnchantmentHelper.hasSilkTouch(player.getMainHandItem()) || !player.getMainHandItem().isCorrectToolForDrops(blockState)) return 0;
        return originalScore;
    }

    private int husbandryScoreOf(Player player, BlockState blockState, Integer originalScore) {
        if (!(blockState.getBlock() instanceof CropBlock crop)) { return 0; }
        return !crop.isMaxAge(blockState) ? 0 : originalScore * (1 + crop.getMaxAge());
    }
}
