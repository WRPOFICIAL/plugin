package net.wrpnetwork.survivalcore.economy;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShopHolder implements InventoryHolder {
    private final boolean isBuy;
    private final String category;
    private final boolean isCategoryView;

    public ShopHolder(boolean isBuy, String category, boolean isCategoryView) {
        this.isBuy = isBuy;
        this.category = category;
        this.isCategoryView = isCategoryView;
    }

    public boolean isBuy() { return isBuy; }
    public String getCategory() { return category; }
    public boolean isCategoryView() { return isCategoryView; }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
