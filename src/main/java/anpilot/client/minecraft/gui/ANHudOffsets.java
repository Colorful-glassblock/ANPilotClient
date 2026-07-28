package anpilot.client.minecraft.gui;

import anpilot.client.bootstrap.ANServiceRegistry;

public final class ANHudOffsets {
    private static final int HOTBAR_STATUS_OFFSET = 10;

    private ANHudOffsets() {
    }

    public static int hotBarStatusOffset() {
        if (!ANServiceRegistry.INSTANCE.isInitialized()) return 0;
        var hotBar = ANServiceRegistry.INSTANCE.getRuntime().getModuleManager().get("HotBar");
        return hotBar != null && hotBar.getEnabled() ? HOTBAR_STATUS_OFFSET : 0;
    }
}
