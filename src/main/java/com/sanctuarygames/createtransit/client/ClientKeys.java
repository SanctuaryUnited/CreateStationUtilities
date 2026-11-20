package com.sanctuarygames.createtransit.client;

/** Client-only key state, kept apart so common code never loads client classes on a server. */
public final class ClientKeys {
    private ClientKeys() {}
    public static boolean shift() { return net.minecraft.client.gui.screens.Screen.hasShiftDown(); }
}
