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
 * Il dump scatta a ogni cambio di contenuto, non solo all'apertura di una
 * GUI nuova: cosi' vengono mappate anche le pagine successive e le
 * schermate di acquisto, che il server riempie dopo averle aperte.
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

    private final Setting<Boolean> saltaVuote = sgGeneral.add(new BoolSetting.Builder()
        .name("salta-gui-vuote")
        .description("Non stampare le GUI ancora vuote, che il server deve finire di riempire")
        .defaultValue(true)
        .build());

    /** Contenuto gia' stampato, per non ripetere lo stesso dump a ogni tick. */
    private String ultimoDump = null;

    public ShopSlotDump() {
        super(WgfAddon.CATEGORY, "shop-slot-dump", "Stampa in chat lo slot di ogni item della GUI aperta");
    }

    @Override
    public void onActivate() {
        ultimoDump = null;
        ChatUtils.info("WGF", "Apri la GUI dello shop: la mappa degli slot compare qui.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            ultimoDump = null;
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        String firma = firma(handler);
        if (firma == null || firma.equals(ultimoDump)) return;

        // Appena aperta, una GUI e' vuota finche' il server non la riempie:
        // stamparla in quel momento darebbe "0 slot pieni" e basta.
        if (saltaVuote.get() && contaPieni(handler) == 0) return;

        ultimoDump = firma;
        dump(handler);
    }

    private void dump(ScreenHandler handler) {
        int pieni = 0;

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
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

    /** Quanti slot del contenitore hanno davvero un item dentro. */
    private int contaPieni(ScreenHandler handler) {
        int pieni = 0;
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) break;
            if (!slot.getStack().isEmpty()) pieni++;
        }
        return pieni;
    }

    /** Firma del contenuto, per accorgersi quando la GUI cambia davvero. */
    private String firma(ScreenHandler handler) {
        StringBuilder sb = new StringBuilder();
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) break;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) sb.append('-');
            else sb.append(stack.getItem()).append(':').append(stack.getName().getString());
            sb.append('|');
        }
        return sb.toString();
    }

    private void riga(int indice, String testo) {
        // ChatUtils passa il messaggio a String.format, quindi le % vanno protette
        ChatUtils.info("WGF", "slot " + indice + " = " + testo.replace("%", "%%"));
    }
}
