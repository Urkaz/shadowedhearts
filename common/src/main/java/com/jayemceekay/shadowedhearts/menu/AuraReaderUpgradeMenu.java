package com.jayemceekay.shadowedhearts.menu;

import com.jayemceekay.shadowedhearts.content.upgrades.IAuraUpgrade;
import com.jayemceekay.shadowedhearts.integration.accessories.SnagAccessoryBridgeHolder;
import com.jayemceekay.shadowedhearts.registry.ModItems;
import com.jayemceekay.shadowedhearts.registry.ModMenuTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Basic container for Aura Reader upgrades. Framework only: accepts items, no server logic yet.
 */
public class AuraReaderUpgradeMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 6;
    private static final String UPGRADE_ROOT = "ReaderUpgrades";
    private static final String UPGRADE_KEY_PREFIX = "U"; // U0..U5

    private final Container upgrades;
    private final Player player;
    private final ItemStack boundReader;

    public AuraReaderUpgradeMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AURA_READER_UPGRADES.get(), containerId);
        this.player = playerInventory.player;
        // Resolve which Aura Reader stack we're editing: prefer main hand, then offhand, then equipped/accessory
        this.boundReader = resolveBoundReader(this.player);

        // 6 upgrade slots (processor, battery, lens, chassis1-3)
        this.upgrades = new SimpleContainer(SLOT_COUNT) {
            @Override
            public boolean canPlaceItem(int index, ItemStack stack) {
                return true;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                // Persist on any change
                saveToStack(boundReader, this, player.level().registryAccess());
            }
        };

        // Load any existing upgrades from the bound reader stack
        loadFromStack(this.boundReader, this.upgrades, player.level().registryAccess());

        // Layout: top row for upgrades (only accept IAuraUpgrade items; never the Aura Reader itself)
        int startX = 44;
        int y = 20;
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.addSlot(new Slot(upgrades, i, startX + i * 18, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // Disallow the Aura Reader itself
                    if (stack.is(ModItems.AURA_READER.get())) return false;
                    // Only allow items that are declared upgrades
                    return stack.getItem() instanceof IAuraUpgrade;
                }
            });
        }

        // Player inventory
        int playerInvY = 51;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int upgradeSlots = SLOT_COUNT;
            int playerInvStart = upgradeSlots;
            int playerInvEnd = playerInvStart + 27;
            int hotbarStart = playerInvEnd;
            int hotbarEnd = hotbarStart + 9;

            if (index < upgradeSlots) {
                // From upgrades to player inventory
                if (!this.moveItemStackTo(stackInSlot, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < hotbarEnd) {
                // From player inventory to upgrades
                if (!this.moveItemStackTo(stackInSlot, 0, upgradeSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Final save on close to be safe
        saveToStack(boundReader, this.upgrades, player.level().registryAccess());
    }

    private static ItemStack resolveBoundReader(Player player) {
        // Prefer main hand if it is the Aura Reader
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.AURA_READER.get())) return main;
        // Then offhand
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.AURA_READER.get())) return off;
        // Accessories or equipped head slot
        ItemStack accessories = SnagAccessoryBridgeHolder.INSTANCE.getAuraReaderStack(player);
        if (!accessories.isEmpty()) return accessories;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.AURA_READER.get())) return head;
        return ItemStack.EMPTY; // fallback, shouldn't happen normally
    }

    private static void loadFromStack(ItemStack reader, Container upgrades, RegistryAccess registries) {
        if (reader.isEmpty()) return;
        CustomData data = reader.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag root = data.copyTag();
        CompoundTag upg = root.getCompound(UPGRADE_ROOT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = UPGRADE_KEY_PREFIX + i;
            if (upg.contains(key)) {
                Tag itemTag = upg.get(key);
                if (itemTag instanceof CompoundTag ct) {
                    java.util.Optional<ItemStack> parsed = ItemStack.parse(registries, ct);
                    upgrades.setItem(i, parsed.orElse(ItemStack.EMPTY));
                } else {
                    upgrades.setItem(i, ItemStack.EMPTY);
                }
            } else {
                upgrades.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void saveToStack(ItemStack reader, Container upgrades, RegistryAccess registries) {
        if (reader.isEmpty()) return;
        CustomData current = reader.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = current != null ? current.copyTag() : new CompoundTag();
        CompoundTag upg = root.getCompound(UPGRADE_ROOT);
        // Clear existing keys first
        for (int i = 0; i < SLOT_COUNT; i++) {
            upg.remove(UPGRADE_KEY_PREFIX + i);
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack st = upgrades.getItem(i);
            if (!st.isEmpty()) {
                Tag enc = st.save(registries);
                upg.put(UPGRADE_KEY_PREFIX + i, enc);
            }
        }
        root.put(UPGRADE_ROOT, upg);
        reader.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
