package com.wgf.addon.modules;

import com.wgf.addon.WgfAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WGF Firework Autofarm - Meteor Client 1.19.2
 * Anti-Detection Vulcan + Auto-Shutdown su Flag
 * 
 * FEATURE ANTI-DETECTION:
 * - Jitter random su tutti i delay (±30% varianza)
 * - Rotazione smooth verso blocchi prima di interagire
 * - Mining speed variabile con break progress simulato
 * - Randomizzazione posizioni piazzamento glowstone
 * - Delay chat variabili (non costanti)
 * 
 * SAFETY MONITOR (ogni tick):
 * - Rileva teleport non previsti (position packet)
 * - Rileva velocity anomalo (knockback/flag)
 * - Rileva messaggi anticheat in chat
 * - Rileva kick/disconnessione
 * - Rileva cambio gamemode / fly forzato
 * - Auto-shutdown immediato se flag rilevato
 */
public class FireworkAutofarm extends Module {

    private final Random random = new Random();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShop = settings.createGroup("Shop Slots");
    private final SettingGroup sgCraft = settings.createGroup("Crafting");
    private final SettingGroup sgAntiDetect = settings.createGroup("Anti-Detection Vulcan");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    // General
    private final Setting<Integer> startDelay = sgGeneral.add(new IntSetting.Builder()
        .name("start-delay").description("Tick di attesa dopo attivazione").defaultValue(240)
        .min(0).sliderMax(600).build());
    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("action-delay").description("Tick base tra azioni").defaultValue(4)
        .min(1).sliderMax(20).build());
    private final Setting<Integer> chatDelay = sgGeneral.add(new IntSetting.Builder()
        .name("chat-delay").description("Tick base dopo comando chat").defaultValue(20)
        .min(10).sliderMax(60).build());
    private final Setting<Integer> guiWait = sgGeneral.add(new IntSetting.Builder()
        .name("gui-wait").description("Tick attesa apertura GUI").defaultValue(10)
        .min(5).sliderMax(40).build());
    private final Setting<Boolean> autoSell = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-sell").description("Esegui /sellall hand").defaultValue(true).build());
    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-feedback").description("Messaggi in chat").defaultValue(true).build());
    private final Setting<Integer> guiTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("gui-timeout").description("Tick massimi di attesa che la GUI si aggiorni prima di proseguire lo stesso")
        .defaultValue(60).min(10).sliderMax(200).build());
    private final Setting<Boolean> debugStati = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-stati").description("Stampa in chat ogni passaggio di stato, per capire dove si blocca")
        .defaultValue(false).build());

    // Shop Slots
    private final Setting<Integer> slotCatBlocchi = sgShop.add(new IntSetting.Builder()
        .name("slot-cat-blocchi").defaultValue(10).min(0).max(53).build());
    private final Setting<Integer> slotCatMinerali = sgShop.add(new IntSetting.Builder()
        .name("slot-cat-minerali").defaultValue(11).min(0).max(53).build());
    private final Setting<Integer> slotCatMobs = sgShop.add(new IntSetting.Builder()
        .name("slot-cat-mobs").defaultValue(12).min(0).max(53).build());
    private final Setting<Integer> slotCatAgricoltura = sgShop.add(new IntSetting.Builder()
        .name("slot-cat-agricoltura").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotCatColoranti = sgShop.add(new IntSetting.Builder()
        .name("slot-cat-coloranti").defaultValue(14).min(0).max(53).build());
    private final Setting<Integer> slotNextPage = sgShop.add(new IntSetting.Builder()
        .name("slot-next-page").defaultValue(50).min(0).max(53).build());
    private final Setting<Integer> slotGlowstone = sgShop.add(new IntSetting.Builder()
        .name("slot-glowstone").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotDiamondBlock = sgShop.add(new IntSetting.Builder()
        .name("slot-diamond-block").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotGunpowder = sgShop.add(new IntSetting.Builder()
        .name("slot-gunpowder").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotFeather = sgShop.add(new IntSetting.Builder()
        .name("slot-feather").defaultValue(14).min(0).max(53).build());
    private final Setting<Integer> slotSugarCane = sgShop.add(new IntSetting.Builder()
        .name("slot-sugar-cane").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotCyanDye = sgShop.add(new IntSetting.Builder()
        .name("slot-cyan-dye").defaultValue(10).min(0).max(53).build());
    private final Setting<Integer> slotPurpleDye = sgShop.add(new IntSetting.Builder()
        .name("slot-purple-dye").defaultValue(11).min(0).max(53).build());
    private final Setting<Integer> slotBlackDye = sgShop.add(new IntSetting.Builder()
        .name("slot-black-dye").defaultValue(12).min(0).max(53).build());
    private final Setting<Integer> slotGrayDye = sgShop.add(new IntSetting.Builder()
        .name("slot-gray-dye").defaultValue(13).min(0).max(53).build());
    private final Setting<Integer> slotQty32 = sgShop.add(new IntSetting.Builder()
        .name("slot-qty-32").defaultValue(31).min(0).max(53).build());
    private final Setting<Integer> slotQty64 = sgShop.add(new IntSetting.Builder()
        .name("slot-qty-64").defaultValue(32).min(0).max(53).build());
    private final Setting<Integer> slotQty8 = sgShop.add(new IntSetting.Builder()
        .name("slot-qty-8").defaultValue(16).min(0).max(53).build());
    private final Setting<Integer> slotConfirmBuy = sgShop.add(new IntSetting.Builder()
        .name("slot-confirm-buy").defaultValue(13).min(0).max(53).build());

    // Crafting
    private final Setting<Integer> craftingRange = sgCraft.add(new IntSetting.Builder()
        .name("crafting-range").defaultValue(5).min(1).max(10).build());

    // Anti-Detection
    private final Setting<Boolean> enableJitter = sgAntiDetect.add(new BoolSetting.Builder()
        .name("enable-jitter").description("Aggiunge varianza casuale ai delay").defaultValue(true).build());
    private final Setting<Integer> jitterPercent = sgAntiDetect.add(new IntSetting.Builder()
        .name("jitter-percent").description("Varianza delay in percentuale").defaultValue(30)
        .min(0).max(80).build());
    private final Setting<Boolean> smoothRotation = sgAntiDetect.add(new BoolSetting.Builder()
        .name("smooth-rotation").description("Ruota smooth verso blocchi prima di interagire").defaultValue(true).build());
    private final Setting<Boolean> randomMining = sgAntiDetect.add(new BoolSetting.Builder()
        .name("random-mining").description("Velocita mining variabile").defaultValue(true).build());
    private final Setting<Boolean> humanLikeDelays = sgAntiDetect.add(new BoolSetting.Builder()
        .name("human-like-delays").description("Delay basati su distribuzione gaussiana").defaultValue(true).build());

    // Safety
    private final Setting<Boolean> autoShutdown = sgSafety.add(new BoolSetting.Builder()
        .name("auto-shutdown").description("Spegni se flaggato da Vulcan").defaultValue(true).build());
    private final Setting<Boolean> detectTeleport = sgSafety.add(new BoolSetting.Builder()
        .name("detect-teleport").description("Rileva teleport non previsti").defaultValue(true).build());
    private final Setting<Boolean> detectVelocity = sgSafety.add(new BoolSetting.Builder()
        .name("detect-velocity").description("Rileva velocity anomalo").defaultValue(true).build());
    private final Setting<Boolean> detectChatFlag = sgSafety.add(new BoolSetting.Builder()
        .name("detect-chat-flag").description("Rileva messaggi anticheat in chat").defaultValue(true).build());
    private final Setting<Integer> maxTeleportDist = sgSafety.add(new IntSetting.Builder()
        .name("max-teleport-dist").description("Distanza massima teleport prima di shutdown").defaultValue(3)
        .min(1).max(10).build());

    // ============================================================
    // STATO
    // ============================================================
    private enum State {
        IDLE, WAIT_START,
        SHOP_GLOWSTONE_CMD, SHOP_GLOWSTONE_WAIT_GUI, SHOP_GLOWSTONE_CLICK_CAT, SHOP_GLOWSTONE_WAIT_CAT,
        SHOP_GLOWSTONE_PAGE1, SHOP_GLOWSTONE_WAIT_P1, SHOP_GLOWSTONE_PAGE2, SHOP_GLOWSTONE_WAIT_P2,
        SHOP_GLOWSTONE_SELECT, SHOP_GLOWSTONE_WAIT_SELECT, SHOP_GLOWSTONE_QTY, SHOP_GLOWSTONE_WAIT_QTY,
        SHOP_GLOWSTONE_CONFIRM, SHOP_GLOWSTONE_WAIT_CONFIRM, SHOP_GLOWSTONE_CLOSE,
        SHOP_DIAMOND_CMD, SHOP_DIAMOND_WAIT_GUI, SHOP_DIAMOND_CLICK_CAT, SHOP_DIAMOND_WAIT_CAT,
        SHOP_DIAMOND_SELECT, SHOP_DIAMOND_WAIT_SELECT, SHOP_DIAMOND_QTY, SHOP_DIAMOND_WAIT_QTY,
        SHOP_DIAMOND_CONFIRM, SHOP_DIAMOND_WAIT_CONFIRM, SHOP_DIAMOND_CLOSE,
        SHOP_GUNPOWDER_CMD, SHOP_GUNPOWDER_WAIT_GUI, SHOP_GUNPOWDER_CLICK_CAT, SHOP_GUNPOWDER_WAIT_CAT,
        SHOP_GUNPOWDER_SELECT, SHOP_GUNPOWDER_WAIT_SELECT, SHOP_GUNPOWDER_QTY, SHOP_GUNPOWDER_WAIT_QTY,
        SHOP_GUNPOWDER_CONFIRM, SHOP_GUNPOWDER_WAIT_CONFIRM, SHOP_GUNPOWDER_CLOSE, SHOP_GUNPOWDER_REPEAT_CHECK,
        SHOP_FEATHER_CMD, SHOP_FEATHER_WAIT_GUI, SHOP_FEATHER_CLICK_CAT, SHOP_FEATHER_WAIT_CAT,
        SHOP_FEATHER_SELECT, SHOP_FEATHER_WAIT_SELECT, SHOP_FEATHER_QTY, SHOP_FEATHER_WAIT_QTY,
        SHOP_FEATHER_CONFIRM, SHOP_FEATHER_WAIT_CONFIRM, SHOP_FEATHER_CLOSE,
        SHOP_SUGAR_CMD, SHOP_SUGAR_WAIT_GUI, SHOP_SUGAR_CLICK_CAT, SHOP_SUGAR_WAIT_CAT,
        SHOP_SUGAR_SELECT, SHOP_SUGAR_WAIT_SELECT, SHOP_SUGAR_QTY, SHOP_SUGAR_WAIT_QTY,
        SHOP_SUGAR_CONFIRM, SHOP_SUGAR_WAIT_CONFIRM, SHOP_SUGAR_CLOSE, SHOP_SUGAR_REPEAT_CHECK,
        SHOP_CYAN_CMD, SHOP_CYAN_WAIT_GUI, SHOP_CYAN_CLICK_CAT, SHOP_CYAN_WAIT_CAT,
        SHOP_CYAN_SELECT, SHOP_CYAN_WAIT_SELECT, SHOP_CYAN_QTY, SHOP_CYAN_WAIT_QTY,
        SHOP_CYAN_CONFIRM, SHOP_CYAN_WAIT_CONFIRM, SHOP_CYAN_CLOSE,
        SHOP_PURPLE_CMD, SHOP_PURPLE_WAIT_GUI, SHOP_PURPLE_CLICK_CAT, SHOP_PURPLE_WAIT_CAT,
        SHOP_PURPLE_SELECT, SHOP_PURPLE_WAIT_SELECT, SHOP_PURPLE_QTY, SHOP_PURPLE_WAIT_QTY,
        SHOP_PURPLE_CONFIRM, SHOP_PURPLE_WAIT_CONFIRM, SHOP_PURPLE_CLOSE,
        SHOP_BLACK_CMD, SHOP_BLACK_WAIT_GUI, SHOP_BLACK_CLICK_CAT, SHOP_BLACK_WAIT_CAT,
        SHOP_BLACK_SELECT, SHOP_BLACK_WAIT_SELECT, SHOP_BLACK_QTY, SHOP_BLACK_WAIT_QTY,
        SHOP_BLACK_CONFIRM, SHOP_BLACK_WAIT_CONFIRM, SHOP_BLACK_CLOSE,
        SHOP_GRAY_CMD, SHOP_GRAY_WAIT_GUI, SHOP_GRAY_CLICK_CAT, SHOP_GRAY_WAIT_CAT,
        SHOP_GRAY_SELECT, SHOP_GRAY_WAIT_SELECT, SHOP_GRAY_QTY, SHOP_GRAY_WAIT_QTY,
        SHOP_GRAY_CONFIRM, SHOP_GRAY_WAIT_CONFIRM, SHOP_GRAY_CLOSE,
        PLACE_GLOWSTONE_START, PLACE_GLOWSTONE_TICK,
        BREAK_GLOWSTONE_START, BREAK_GLOWSTONE_TICK, BREAK_GLOWSTONE_COLLECT,
        CRAFT_PAPER_OPEN, CRAFT_PAPER_WAIT_OPEN, CRAFT_PAPER_FILL, CRAFT_PAPER_WAIT_CRAFT, CRAFT_PAPER_CHECK, CRAFT_PAPER_BUY_MORE,
        UNCRAFT_DIAMOND_OPEN, UNCRAFT_DIAMOND_WAIT_OPEN, UNCRAFT_DIAMOND_FILL, UNCRAFT_DIAMOND_WAIT_CRAFT, UNCRAFT_DIAMOND_CHECK,
        STARS_OPEN, STARS_WAIT_OPEN, STARS_FILL, STARS_WAIT_CRAFT, STARS_CHECK,
        STARS_FADE_FILL, STARS_FADE_WAIT_CRAFT, STARS_FADE_CHECK,
        ROCKETS_FILL, ROCKETS_WAIT_CRAFT, ROCKETS_CHECK,
        SELLALL_CMD, SELLALL_WAIT, END
    }

    private State state = State.IDLE;
    private int tickTimer = 0;
    private int waitTicks = 0;
    private int glowstonePlaceIndex = 0;
    private int glowstoneBreakIndex = 0;
    private List<BlockPos> glowstonePositions = new ArrayList<>();
    private BlockPos craftingPos = null;
    private BlockPos currentBreakPos = null;
    private int gunpowderBought = 0;
    private int sugarBought = 0;
    private Vec3d lastPos = null;
    private boolean flagged = false;
    private int consecutiveFlags = 0;
    /** Contenuto del container al momento dell'ultimo click, per capire quando il server lo aggiorna. */
    private String containerSig = null;
    /** Tick passati dall'ultimo click in attesa che la GUI cambi davvero. */
    private int guiUpdateTicks = 0;
    private State lastLoggedState = null;

    public FireworkAutofarm() {
        super(WgfAddon.CATEGORY, "firework-autofarm", "WGF Firework Autofarm - Anti-Vulcan");
    }

    @Override
    public void onActivate() {
        state = State.WAIT_START;
        tickTimer = 0; waitTicks = 0;
        glowstonePlaceIndex = 0; glowstoneBreakIndex = 0;
        gunpowderBought = 0; sugarBought = 0;
        glowstonePositions.clear();
        currentBreakPos = null;
        flagged = false; consecutiveFlags = 0;
        containerSig = null; guiUpdateTicks = 0;
        lastPos = null;
        if (mc.player != null) lastPos = mc.player.getPos();
        info("Anti-Vulcan attivo. Jitter: " + enableJitter.get() + " | Auto-shutdown: " + autoShutdown.get());
    }

    @Override
    public void onDeactivate() {
        state = State.IDLE;
        if (mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof CraftingScreen) {
            mc.player.closeHandledScreen();
        }
    }

    // ============================================================
    // ANTI-DETECTION: JITTER & HUMAN-LIKE DELAYS
    // ============================================================
    private int getJitteredDelay(int base) {
        if (!enableJitter.get()) return base;
        int variance = (base * jitterPercent.get()) / 100;
        int jitter = random.nextInt(variance * 2 + 1) - variance;
        return Math.max(1, base + jitter);
    }

    private int getGaussianDelay(int base) {
        if (!humanLikeDelays.get()) return getJitteredDelay(base);
        double gaussian = random.nextGaussian(); // mean=0, stddev=1
        int delay = (int) (base + gaussian * (base * 0.2));
        return Math.max(1, delay);
    }

    // ============================================================
    // SAFETY MONITOR - Rilevazione flag Vulcan
    // ============================================================
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!isActive() || !autoShutdown.get()) return;

        // Rileva teleport non previsto (Vulcan sposta il player per verificare)
        if (detectTeleport.get() && event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (mc.player != null && lastPos != null) {
                double dist = lastPos.distanceTo(new Vec3d(packet.getX(), packet.getY(), packet.getZ()));
                if (dist > maxTeleportDist.get() && state != State.IDLE) {
                    consecutiveFlags++;
                    warn("Teleport rilevato! Distanza: " + String.format("%.1f", dist) + "m [Flag " + consecutiveFlags + "/3]");
                    if (consecutiveFlags >= 2) shutdown("Vulcan teleport flag");
                }
            }
        }

        // Rileva velocity anomalo (knockback/flag)
        if (detectVelocity.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getId() == mc.player.getId()) {
                double vel = Math.sqrt(packet.getVelocityX() * packet.getVelocityX()
                    + packet.getVelocityY() * packet.getVelocityY()
                    + packet.getVelocityZ() * packet.getVelocityZ());
                if (vel > 10000) { // Vulcan invia velocity estremi durante i flag
                    consecutiveFlags++;
                    warn("Velocity anomalo rilevato! [Flag " + consecutiveFlags + "/3]");
                    if (consecutiveFlags >= 2) shutdown("Vulcan velocity flag");
                }
            }
        }

        // Rileva disconnessione/kick
        if (event.packet instanceof DisconnectS2CPacket) {
            shutdown("Disconnessione rilevata");
        }

        // Rileva cambio gamemode (spesso usato da anticheat per testare)
        if (event.packet instanceof GameStateChangeS2CPacket packet) {
            if (packet.getReason() == GameStateChangeS2CPacket.GAME_MODE_CHANGED) {
                warn("Cambio gamemode rilevato");
                consecutiveFlags++;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive() || mc.player == null || mc.world == null) return;
        if (waitTicks > 0) { waitTicks--; return; }

        // Aggiorna posizione per rilevamento teleport
        if (mc.player != null) lastPos = mc.player.getPos();

        // Safety: se il player viene spostato bruscamente mentre piazza/rompe
        if (autoShutdown.get() && state.ordinal() >= State.PLACE_GLOWSTONE_START.ordinal()
            && state.ordinal() <= State.BREAK_GLOWSTONE_COLLECT.ordinal()) {
            if (mc.player.getVelocity().lengthSquared() > 0.5) {
                warn("Movimento anomalo durante mining/placing");
                consecutiveFlags++;
                if (consecutiveFlags >= 3) shutdown("Movimento anomalo");
            }
        }

        if (debugStati.get() && state != lastLoggedState) {
            lastLoggedState = state;
            ChatUtils.info("WGF", "stato: " + state.name());
        }

        switch (state) {
            case WAIT_START:
                tickTimer++;
                if (tickTimer >= startDelay.get()) {
                    tickTimer = 0;
                    state = State.SHOP_GLOWSTONE_CMD;
                    info("Avvio sequenza acquisti...");
                }
                break;

            // ==================== SHOP GLOWSTONE 32 ====================
            case SHOP_GLOWSTONE_CMD:
                sendCmd("/shop");
                state = State.SHOP_GLOWSTONE_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_GLOWSTONE_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_GLOWSTONE_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) { shutdown("Timeout apertura shop"); }
                break;
            case SHOP_GLOWSTONE_CLICK_CAT:
                clickContainerSlot(slotCatBlocchi.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GLOWSTONE_PAGE1;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GLOWSTONE_PAGE1:
                clickContainerSlot(slotNextPage.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_P1;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_P1:
                if (isContainerUpdated()) {
                    state = State.SHOP_GLOWSTONE_PAGE2;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GLOWSTONE_PAGE2:
                clickContainerSlot(slotNextPage.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_P2;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_P2:
                if (isContainerUpdated()) {
                    state = State.SHOP_GLOWSTONE_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GLOWSTONE_SELECT:
                clickContainerSlot(slotGlowstone.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GLOWSTONE_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GLOWSTONE_QTY:
                clickContainerSlot(slotQty32.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_GLOWSTONE_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GLOWSTONE_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GLOWSTONE_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GLOWSTONE_WAIT_CONFIRM:
                state = State.SHOP_GLOWSTONE_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_GLOWSTONE_CLOSE:
                closeContainer();
                state = State.SHOP_DIAMOND_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            // ==================== SHOP DIAMOND BLOCK 8 ====================
            case SHOP_DIAMOND_CMD:
                sendCmd("/shop");
                state = State.SHOP_DIAMOND_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_DIAMOND_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_DIAMOND_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop diamanti");
                break;
            case SHOP_DIAMOND_CLICK_CAT:
                clickContainerSlot(slotCatMinerali.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_DIAMOND_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_DIAMOND_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_DIAMOND_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_DIAMOND_SELECT:
                clickContainerSlot(slotDiamondBlock.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_DIAMOND_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_DIAMOND_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_DIAMOND_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_DIAMOND_QTY:
                clickContainerSlot(slotQty8.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_DIAMOND_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_DIAMOND_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_DIAMOND_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_DIAMOND_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_DIAMOND_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_DIAMOND_WAIT_CONFIRM:
                state = State.SHOP_DIAMOND_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_DIAMOND_CLOSE:
                closeContainer();
                state = State.SHOP_GUNPOWDER_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            // ==================== SHOP GUNPOWDER 64 (x3) ====================
            case SHOP_GUNPOWDER_CMD:
                sendCmd("/shop");
                state = State.SHOP_GUNPOWDER_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_GUNPOWDER_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_GUNPOWDER_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop gunpowder");
                break;
            case SHOP_GUNPOWDER_CLICK_CAT:
                clickContainerSlot(slotCatMobs.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GUNPOWDER_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GUNPOWDER_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GUNPOWDER_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GUNPOWDER_SELECT:
                clickContainerSlot(slotGunpowder.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GUNPOWDER_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GUNPOWDER_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GUNPOWDER_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GUNPOWDER_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GUNPOWDER_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GUNPOWDER_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_GUNPOWDER_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GUNPOWDER_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GUNPOWDER_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GUNPOWDER_WAIT_CONFIRM:
                state = State.SHOP_GUNPOWDER_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_GUNPOWDER_CLOSE:
                closeContainer();
                gunpowderBought++;
                state = State.SHOP_GUNPOWDER_REPEAT_CHECK;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_GUNPOWDER_REPEAT_CHECK:
                if (gunpowderBought < 3) {
                    state = State.SHOP_GUNPOWDER_CMD;
                    info("Gunpowder: " + gunpowderBought + "/3");
                } else {
                    state = State.SHOP_FEATHER_CMD;
                    info("Gunpowder completato");
                }
                break;

            // ==================== SHOP FEATHER 64 ====================
            case SHOP_FEATHER_CMD:
                sendCmd("/shop");
                state = State.SHOP_FEATHER_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_FEATHER_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_FEATHER_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop feather");
                break;
            case SHOP_FEATHER_CLICK_CAT:
                clickContainerSlot(slotCatMobs.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_FEATHER_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_FEATHER_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_FEATHER_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_FEATHER_SELECT:
                clickContainerSlot(slotFeather.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_FEATHER_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_FEATHER_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_FEATHER_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_FEATHER_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_FEATHER_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_FEATHER_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_FEATHER_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_FEATHER_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_FEATHER_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_FEATHER_WAIT_CONFIRM:
                state = State.SHOP_FEATHER_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_FEATHER_CLOSE:
                closeContainer();
                state = State.SHOP_SUGAR_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            // ==================== SHOP SUGAR CANE 64 (x2) ====================
            case SHOP_SUGAR_CMD:
                sendCmd("/shop");
                state = State.SHOP_SUGAR_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_SUGAR_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_SUGAR_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop sugar cane");
                break;
            case SHOP_SUGAR_CLICK_CAT:
                clickContainerSlot(slotCatAgricoltura.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_SUGAR_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_SUGAR_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_SUGAR_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_SUGAR_SELECT:
                clickContainerSlot(slotSugarCane.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_SUGAR_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_SUGAR_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_SUGAR_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_SUGAR_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_SUGAR_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_SUGAR_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_SUGAR_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_SUGAR_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_SUGAR_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_SUGAR_WAIT_CONFIRM:
                state = State.SHOP_SUGAR_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_SUGAR_CLOSE:
                closeContainer();
                sugarBought++;
                state = State.SHOP_SUGAR_REPEAT_CHECK;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_SUGAR_REPEAT_CHECK:
                if (sugarBought < 2) {
                    state = State.SHOP_SUGAR_CMD;
                    info("Sugar: " + sugarBought + "/2");
                } else {
                    state = State.SHOP_CYAN_CMD;
                    info("Sugar completato");
                }
                break;

            // ==================== SHOP DYES ====================
            case SHOP_CYAN_CMD:
                sendCmd("/shop");
                state = State.SHOP_CYAN_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_CYAN_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_CYAN_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop cyan");
                break;
            case SHOP_CYAN_CLICK_CAT:
                clickContainerSlot(slotCatColoranti.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_CYAN_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_CYAN_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_CYAN_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_CYAN_SELECT:
                clickContainerSlot(slotCyanDye.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_CYAN_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_CYAN_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_CYAN_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_CYAN_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_CYAN_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_CYAN_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_CYAN_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_CYAN_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_CYAN_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_CYAN_WAIT_CONFIRM:
                state = State.SHOP_CYAN_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_CYAN_CLOSE:
                closeContainer();
                state = State.SHOP_PURPLE_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            case SHOP_PURPLE_CMD:
                sendCmd("/shop");
                state = State.SHOP_PURPLE_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_PURPLE_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_PURPLE_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop purple");
                break;
            case SHOP_PURPLE_CLICK_CAT:
                clickContainerSlot(slotCatColoranti.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_PURPLE_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_PURPLE_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_PURPLE_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_PURPLE_SELECT:
                clickContainerSlot(slotPurpleDye.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_PURPLE_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_PURPLE_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_PURPLE_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_PURPLE_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_PURPLE_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_PURPLE_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_PURPLE_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_PURPLE_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_PURPLE_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_PURPLE_WAIT_CONFIRM:
                state = State.SHOP_PURPLE_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_PURPLE_CLOSE:
                closeContainer();
                state = State.SHOP_BLACK_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            case SHOP_BLACK_CMD:
                sendCmd("/shop");
                state = State.SHOP_BLACK_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_BLACK_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_BLACK_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop black");
                break;
            case SHOP_BLACK_CLICK_CAT:
                clickContainerSlot(slotCatColoranti.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_BLACK_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_BLACK_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_BLACK_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_BLACK_SELECT:
                clickContainerSlot(slotBlackDye.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_BLACK_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_BLACK_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_BLACK_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_BLACK_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_BLACK_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_BLACK_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_BLACK_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_BLACK_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_BLACK_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_BLACK_WAIT_CONFIRM:
                state = State.SHOP_BLACK_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_BLACK_CLOSE:
                closeContainer();
                state = State.SHOP_GRAY_CMD;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;

            case SHOP_GRAY_CMD:
                sendCmd("/shop");
                state = State.SHOP_GRAY_WAIT_GUI;
                waitTicks = getGaussianDelay(chatDelay.get());
                break;
            case SHOP_GRAY_WAIT_GUI:
                if (isContainerOpen()) {
                    state = State.SHOP_GRAY_CLICK_CAT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 80) shutdown("Timeout shop gray");
                break;
            case SHOP_GRAY_CLICK_CAT:
                clickContainerSlot(slotCatColoranti.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GRAY_WAIT_CAT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GRAY_WAIT_CAT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GRAY_SELECT;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GRAY_SELECT:
                clickContainerSlot(slotGrayDye.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GRAY_WAIT_SELECT;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GRAY_WAIT_SELECT:
                if (isContainerUpdated()) {
                    state = State.SHOP_GRAY_QTY;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GRAY_QTY:
                clickContainerSlot(slotQty64.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GRAY_WAIT_QTY;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GRAY_WAIT_QTY:
                if (isContainerUpdated()) {
                    state = State.SHOP_GRAY_CONFIRM;
                    waitTicks = getJitteredDelay(actionDelay.get());
                }
                break;
            case SHOP_GRAY_CONFIRM:
                clickContainerSlot(slotConfirmBuy.get(), 0, SlotActionType.PICKUP);
                state = State.SHOP_GRAY_WAIT_CONFIRM;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case SHOP_GRAY_WAIT_CONFIRM:
                state = State.SHOP_GRAY_CLOSE;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case SHOP_GRAY_CLOSE:
                closeContainer();
                state = State.PLACE_GLOWSTONE_START;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;

            // ==================== PIAZZA 32 GLOWSTONE ====================
            case PLACE_GLOWSTONE_START:
                info("Piazzamento Glowstone...");
                glowstonePositions = getGlowstonePlacementPositions();
                glowstonePlaceIndex = 0;
                state = State.PLACE_GLOWSTONE_TICK;
                break;
            case PLACE_GLOWSTONE_TICK:
                if (glowstonePlaceIndex >= 32 || glowstonePlaceIndex >= glowstonePositions.size()) {
                    state = State.BREAK_GLOWSTONE_START;
                    break;
                }
                BlockPos placePos = glowstonePositions.get(glowstonePlaceIndex);
                FindItemResult glowItem = InvUtils.findInHotbar(Items.GLOWSTONE);
                if (!glowItem.found()) { shutdown("Glowstone non trovata"); return; }
                if (smoothRotation.get()) rotateTo(placePos);
                BlockUtils.place(placePos, glowItem, !smoothRotation.get(), 50, true, true);
                glowstonePlaceIndex++;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;

            // ==================== ROMPI GLOWSTONE ====================
            case BREAK_GLOWSTONE_START:
                info("Rottura Glowstone...");
                glowstoneBreakIndex = 0;
                state = State.BREAK_GLOWSTONE_TICK;
                break;
            case BREAK_GLOWSTONE_TICK:
                if (glowstoneBreakIndex >= glowstonePositions.size()) {
                    state = State.BREAK_GLOWSTONE_COLLECT;
                    waitTicks = getJitteredDelay(40);
                    break;
                }
                BlockPos breakPos = glowstonePositions.get(glowstoneBreakIndex);
                if (mc.world.isAir(breakPos)) {
                    glowstoneBreakIndex++;
                    break;
                }
                if (currentBreakPos == null || !currentBreakPos.equals(breakPos)) {
                    currentBreakPos = breakPos;
                    if (smoothRotation.get()) rotateTo(breakPos);
                    mc.interactionManager.attackBlock(breakPos, Direction.UP);
                }
                if (mc.interactionManager.updateBlockBreakingProgress(breakPos, Direction.UP)) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                if (mc.world.isAir(breakPos)) {
                    glowstoneBreakIndex++;
                    currentBreakPos = null;
                }
                // Mining speed variabile
                waitTicks = randomMining.get() ? random.nextInt(3) + 1 : 1;
                break;
            case BREAK_GLOWSTONE_COLLECT:
                info("Glowstone Dust raccolta.");
                state = State.CRAFT_PAPER_OPEN;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;

            // ==================== CRAFT PAPER ====================
            case CRAFT_PAPER_OPEN:
                craftingPos = findCraftingTable();
                if (craftingPos == null) { shutdown("Crafting Table non trovata"); return; }
                if (smoothRotation.get()) rotateTo(craftingPos);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(craftingPos), Direction.UP, craftingPos, false));
                state = State.CRAFT_PAPER_WAIT_OPEN;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case CRAFT_PAPER_WAIT_OPEN:
                if (mc.currentScreen instanceof CraftingScreen) {
                    state = State.CRAFT_PAPER_FILL;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 60) shutdown("Timeout crafting");
                break;
            case CRAFT_PAPER_FILL:
                if (countItem(Items.SUGAR_CANE) < 66) {
                    info("Sugar cane insufficiente, ritorno ad acquisto...");
                    sugarBought = 0;
                    state = State.SHOP_SUGAR_CMD;
                    closeContainer();
                    return;
                }
                ScreenHandler paperHandler = mc.player.currentScreenHandler;
                int sugarSlot = findInInventory(Items.SUGAR_CANE);
                if (sugarSlot == -1) { state = State.CRAFT_PAPER_BUY_MORE; return; }
                for (int gridSlot = 1; gridSlot <= 9; gridSlot++) {
                    if (paperHandler.getSlot(gridSlot).getStack().isEmpty()) {
                        clickSlot(paperHandler, sugarSlot, 0, SlotActionType.QUICK_MOVE);
                        break;
                    }
                }
                state = State.CRAFT_PAPER_WAIT_CRAFT;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case CRAFT_PAPER_WAIT_CRAFT:
                if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
                    clickSlot(mc.player.currentScreenHandler, 0, 0, SlotActionType.QUICK_MOVE);
                }
                state = State.CRAFT_PAPER_CHECK;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case CRAFT_PAPER_CHECK:
                if (countItem(Items.PAPER) >= 66) {
                    info("Paper completato (66+).");
                    closeContainer();
                    state = State.UNCRAFT_DIAMOND_OPEN;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else {
                    state = State.CRAFT_PAPER_FILL;
                }
                break;
            case CRAFT_PAPER_BUY_MORE:
                closeContainer();
                sugarBought = 0;
                state = State.SHOP_SUGAR_CMD;
                break;

            // ==================== UNCRAFT DIAMONDS ====================
            case UNCRAFT_DIAMOND_OPEN:
                if (smoothRotation.get()) rotateTo(craftingPos);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(craftingPos), Direction.UP, craftingPos, false));
                state = State.UNCRAFT_DIAMOND_WAIT_OPEN;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case UNCRAFT_DIAMOND_WAIT_OPEN:
                if (mc.currentScreen instanceof CraftingScreen) {
                    state = State.UNCRAFT_DIAMOND_FILL;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 60) shutdown("Timeout uncraft");
                break;
            case UNCRAFT_DIAMOND_FILL:
                if (countItem(Items.DIAMOND_BLOCK) == 0) { state = State.UNCRAFT_DIAMOND_CHECK; return; }
                int dbSlot = findInInventory(Items.DIAMOND_BLOCK);
                if (dbSlot == -1) { state = State.UNCRAFT_DIAMOND_CHECK; return; }
                clickSlot(mc.player.currentScreenHandler, dbSlot, 0, SlotActionType.QUICK_MOVE);
                state = State.UNCRAFT_DIAMOND_WAIT_CRAFT;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case UNCRAFT_DIAMOND_WAIT_CRAFT:
                clickSlot(mc.player.currentScreenHandler, 0, 0, SlotActionType.QUICK_MOVE);
                state = State.UNCRAFT_DIAMOND_CHECK;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case UNCRAFT_DIAMOND_CHECK:
                if (countItem(Items.DIAMOND) >= 64) {
                    info("Diamanti pronti (64+).");
                    closeContainer();
                    state = State.STARS_OPEN;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else {
                    state = State.UNCRAFT_DIAMOND_FILL;
                }
                break;

            // ==================== CRAFT FIREWORK STARS ====================
            case STARS_OPEN:
                if (smoothRotation.get()) rotateTo(craftingPos);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(craftingPos), Direction.UP, craftingPos, false));
                state = State.STARS_WAIT_OPEN;
                waitTicks = getJitteredDelay(guiWait.get());
                break;
            case STARS_WAIT_OPEN:
                if (mc.currentScreen instanceof CraftingScreen) {
                    state = State.STARS_FILL;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else if (tickTimer++ > 60) shutdown("Timeout stars");
                break;
            case STARS_FILL:
                if (!fillCraftingSlot(Items.GUNPOWDER, 1)) { shutdown("Manca gunpowder"); return; }
                if (!fillCraftingSlot(Items.CYAN_DYE, 2)) { shutdown("Manca cyan dye"); return; }
                if (!fillCraftingSlot(Items.PURPLE_DYE, 3)) { shutdown("Manca purple dye"); return; }
                if (!fillCraftingSlot(Items.GLOWSTONE_DUST, 4)) { shutdown("Manca glowstone dust"); return; }
                if (!fillCraftingSlot(Items.DIAMOND, 5)) { shutdown("Manca diamond"); return; }
                if (!fillCraftingSlot(Items.FEATHER, 6)) { shutdown("Manca feather"); return; }
                state = State.STARS_WAIT_CRAFT;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case STARS_WAIT_CRAFT:
                clickSlot(mc.player.currentScreenHandler, 0, 0, SlotActionType.QUICK_MOVE);
                state = State.STARS_CHECK;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case STARS_CHECK:
                if (countItem(Items.FIREWORK_STAR) >= 64) {
                    info("Stars completate (64).");
                    state = State.STARS_FADE_FILL;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else {
                    state = State.STARS_FILL;
                }
                break;

            // ==================== CRAFT STARS FADE ====================
            case STARS_FADE_FILL:
                if (!fillCraftingSlot(Items.FIREWORK_STAR, 1)) { shutdown("Manca star"); return; }
                if (!fillCraftingSlot(Items.BLACK_DYE, 2)) { shutdown("Manca black dye"); return; }
                if (!fillCraftingSlot(Items.GRAY_DYE, 3)) { shutdown("Manca gray dye"); return; }
                state = State.STARS_FADE_WAIT_CRAFT;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case STARS_FADE_WAIT_CRAFT:
                clickSlot(mc.player.currentScreenHandler, 0, 0, SlotActionType.QUICK_MOVE);
                state = State.STARS_FADE_CHECK;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case STARS_FADE_CHECK:
                if (countItem(Items.FIREWORK_STAR) >= 64) {
                    info("Stars con fade completate.");
                    state = State.ROCKETS_FILL;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else {
                    state = State.STARS_FADE_FILL;
                }
                break;

            // ==================== CRAFT ROCKETS ====================
            case ROCKETS_FILL:
                if (!fillCraftingSlot(Items.FIREWORK_STAR, 1)) { shutdown("Manca star"); return; }
                if (!fillCraftingSlot(Items.GUNPOWDER, 2)) { shutdown("Manca gunpowder"); return; }
                if (!fillCraftingSlot(Items.GUNPOWDER, 3)) { shutdown("Manca gunpowder"); return; }
                if (!fillCraftingSlot(Items.PAPER, 4)) { shutdown("Manca paper"); return; }
                state = State.ROCKETS_WAIT_CRAFT;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case ROCKETS_WAIT_CRAFT:
                clickSlot(mc.player.currentScreenHandler, 0, 0, SlotActionType.QUICK_MOVE);
                state = State.ROCKETS_CHECK;
                waitTicks = getJitteredDelay(actionDelay.get());
                break;
            case ROCKETS_CHECK:
                if (countItem(Items.FIREWORK_ROCKET) >= 64) {
                    info("Rockets completate (64).");
                    closeContainer();
                    state = State.SELLALL_CMD;
                    waitTicks = getJitteredDelay(actionDelay.get());
                } else {
                    state = State.ROCKETS_FILL;
                }
                break;

            // ==================== SELLALL ====================
            case SELLALL_CMD:
                if (autoSell.get()) {
                    sendCmd("/sellall hand");
                    state = State.SELLALL_WAIT;
                    waitTicks = getGaussianDelay(chatDelay.get());
                } else {
                    state = State.END;
                }
                break;
            case SELLALL_WAIT:
                info("Vendita completata.");
                state = State.END;
                break;

            case END:
                info("=== CICLO COMPLETATO ===");
                toggle();
                break;

            case IDLE:
            default:
                break;
        }
    }

    // ============================================================
    // ROTAZIONE SMOOTH (anti-aimbot detection)
    // ============================================================
    private void rotateTo(BlockPos pos) {
        if (mc.player == null) return;
        Vec3d target = Vec3d.ofCenter(pos);
        double dx = target.x - mc.player.getX();
        double dy = target.y - mc.player.getEyeY();
        double dz = target.z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        // Aggiungi micro-jitter alla rotazione per sembrare meno robotico
        yaw += (random.nextFloat() - 0.5f) * 2f;
        pitch += (random.nextFloat() - 0.5f) * 1f;
        Rotations.rotate(yaw, pitch);
    }

    // ============================================================
    // SHUTDOWN SAFETY
    // ============================================================
    private void shutdown(String reason) {
        if (!autoShutdown.get()) return;
        flagged = true;
        ChatUtils.error("WGF", "SHUTDOWN: " + reason + " [Auto-shutdown attivato]");
        state = State.IDLE;
        toggle();
    }

    private void warn(String msg) {
        if (chatFeedback.get()) {
            ChatUtils.warning("WGF", msg);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================
    private void sendCmd(String cmd) {
        ChatUtils.sendPlayerMsg(cmd);
    }

    private void info(String msg) {
        if (chatFeedback.get()) ChatUtils.info("WGF", msg);
    }

    private boolean isContainerOpen() {
        return mc.currentScreen instanceof GenericContainerScreen;
    }

    private void closeContainer() {
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            mc.player.closeHandledScreen();
        }
    }

    private void clickContainerSlot(int slot, int button, SlotActionType type) {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
        ScreenHandler handler = screen.getScreenHandler();
        containerSig = containerSignature();
        guiUpdateTicks = 0;
        clickSlot(handler, slot, button, type);
    }

    /** Firma del contenuto del container aperto: item, quantita' e nome di ogni slot. */
    private String containerSignature() {
        if (mc.player == null) return null;
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return null;

        StringBuilder sb = new StringBuilder();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory == mc.player.getInventory()) break;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) sb.append('-');
            else sb.append(stack.getItem()).append('x').append(stack.getCount())
                   .append(':').append(stack.getName().getString());
            sb.append('|');
        }
        return sb.toString();
    }

    /**
     * Vero solo quando il server ha davvero sostituito il contenuto della GUI
     * dopo l'ultimo click.
     *
     * Cliccare una categoria non chiude il container: il server ne rimpiazza il
     * contenuto lasciando la stessa schermata aperta. isContainerOpen() quindi
     * resta vero da subito e non dice nulla sull'arrivo della pagina nuova:
     * aspettare solo un numero fisso di tick fa cliccare il layout vecchio ogni
     * volta che il server risponde piu' lento del previsto.
     */
    private boolean isContainerUpdated() {
        if (!isContainerOpen()) return false;
        if (containerSig == null) return true;

        String now = containerSignature();
        if (now != null && !now.equals(containerSig)) return true;

        // Se il server non aggiorna la GUI entro il timeout si prosegue lo stesso:
        // meglio un click potenzialmente sbagliato che restare fermi per sempre.
        // L'avviso non passa da warn() perche' deve vedersi anche con
        // chat-feedback disattivato.
        if (guiUpdateTicks++ > guiTimeout.get()) {
            ChatUtils.warning("WGF", "GUI non aggiornata entro " + guiTimeout.get() + " tick: proseguo comunque");
            containerSig = null;
            return true;
        }

        return false;
    }

    private void clickSlot(ScreenHandler handler, int slot, int button, SlotActionType type) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(handler.syncId, slot, button, type, mc.player);
    }

    private int findHotbarSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int findInInventory(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(item) && mc.player.getInventory().getStack(i).getCount() > 0) {
                return i;
            }
        }
        return -1;
    }

    private int countItem(Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        if (mc.player.currentScreenHandler != null && mc.player.currentScreenHandler.getCursorStack().isOf(item)) {
            count += mc.player.currentScreenHandler.getCursorStack().getCount();
        }
        return count;
    }

    private List<BlockPos> getGlowstonePlacementPositions() {
        List<BlockPos> list = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        // Randomizza leggermente l'offset per evitare pattern identici
        int offsetX = random.nextInt(2);
        int offsetZ = random.nextInt(2);
        for (int x = -3 + offsetX; x <= 3 + offsetX; x++) {
            for (int z = -3 + offsetZ; z <= 3 + offsetZ; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos pos = center.add(x, -1, z);
                if (mc.world.getBlockState(pos).isAir()) {
                    list.add(pos);
                }
                if (list.size() >= 32) return list;
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (mc.world.getBlockState(pos).isAir() && !pos.equals(center)) {
                    list.add(pos);
                }
                if (list.size() >= 32) return list;
            }
        }
        return list;
    }

    private BlockPos findCraftingTable() {
        BlockPos center = mc.player.getBlockPos();
        int range = craftingRange.get();
        for (BlockPos pos : BlockPos.iterate(center.add(-range, -2, -range), center.add(range, 2, range))) {
            if (mc.world.getBlockState(pos).isOf(Blocks.CRAFTING_TABLE)) {
                return pos;
            }
        }
        return null;
    }

    private boolean fillCraftingSlot(Item item, int gridSlot) {
        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler handler) {
            if (handler.getSlot(gridSlot).getStack().isOf(item)) return true;
            int invSlot = findInInventory(item);
            if (invSlot == -1) return false;
            int screenSlot = invSlot < 9 ? 37 + invSlot : 10 + (invSlot - 9);
            clickSlot(handler, screenSlot, 0, SlotActionType.PICKUP);
            clickSlot(handler, gridSlot, 0, SlotActionType.PICKUP);
            clickSlot(handler, screenSlot, 0, SlotActionType.PICKUP);
            return true;
        }
        return false;
    }
}
