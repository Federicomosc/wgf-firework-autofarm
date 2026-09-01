package com.wgf.addon.modules;

import com.wgf.addon.WgfAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * WGF Shop Slot Dump
 *
 * Stampa in chat la mappa degli slot della GUI attualmente aperta:
 * per ogni slot del contenitore scrive l'indice e il nome dell'item.
 *
 * Serve a ricavare i numeri esatti da mettere nel gruppo "Shop Slots"
 * di FireworkAutofarm quando il server ha un layout diverso da quello
 * per cui i default sono tarati. Attivalo, apri /shop, leggi la chat.
 *
 * Gli slot dell'inventario del giocatore non vengono elencati: contano
 * solo quelli del contenitore, che sono anche gli unici cliccabili dal
 * modulo di autofarm.
 */
public class ShopSlotDump extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> soloPieni = sgGeneral.add(new BoolSetting.Builder()
        .name("solo-slot-pieni")
        .description("Elenca solo gli slot che contengono un item")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> mostraQuantita = sgGeneral.add(new BoolSetting.Builder()
        .name("mostra-quantita")
        .description("Aggiunge la dimensione dello stack")
        .defaultValue(false)
        .build());

    /** syncId della GUI gia' stampata, per non ripetere il dump a ogni tick. */
    private int lastSyncId = -1;

    public ShopSlotDump() {
        super(WgfAddon.CATEGORY, "shop-slot-dump", "Stampa in chat lo slot di ogni item della GUI aperta");
    }

    @Override
    public void onActivate() {
        lastSyncId = -1;
        ChatUtils.info("WGF", "Apri la GUI dello shop: la mappa degli slot compare qui.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            lastSyncId = -1;
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (handler.syncId == lastSyncId) return;
        lastSyncId = handler.syncId;

        dump(handler);
    }

    private void dump(ScreenHandler handler) {
        if (mc.player == null) return;

        int pieni = 0;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);

            // gli slot del contenitore precedono sempre quelli del giocatore
            if (slot.inventory == mc.player.getInventory()) break;

            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) {
                if (!soloPieni.get()) riga(i, "(vuoto)");
                continue;
            }

            String nome = stack.getName().getString();
            if (mostraQuantita.get()) nome = nome + " x" + stack.getCount();

            riga(i, nome);
            pieni++;
        }

        ChatUtils.info("WGF", "--- fine mappa: " + pieni + " slot pieni ---");
    }

    private void riga(int indice, String testo) {
        // ChatUtils passa il messaggio a String.format, quindi le % vanno protette
        ChatUtils.info("WGF", "slot " + indice + " = " + testo.replace("%", "%%"));
    }
}
