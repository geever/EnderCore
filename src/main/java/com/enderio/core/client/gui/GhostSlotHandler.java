package com.enderio.core.client.gui;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.client.gui.widget.GhostSlot;
import com.enderio.core.client.render.RenderUtil;
import com.enderio.core.common.util.ItemUtil;
import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class GhostSlotHandler {

  protected final @Nonnull List<GhostSlot> ghostSlots = Lists.newArrayList();
  protected @Nullable GhostSlot hoverGhostSlot;

  public GhostSlotHandler() {
  }

  // GhostSlot managing

  protected @Nonnull List<GhostSlot> getGhostSlots() {
    return ghostSlots;
  }

  protected GhostSlot getGhostSlotAt(@Nonnull GuiContainerBase guiContainerBase, int mouseX, int mouseY) {
    int mX = mouseX - guiContainerBase.getGuiLeft();
    int mY = mouseY - guiContainerBase.getGuiTop();
    for (GhostSlot slot : ghostSlots) {
      if (slot.isVisible() && slot.isMouseOver(mX, mY)) {
        return slot;
      }
    }
    return null;
  }

  // Slot interaction

  /**
   * Called when a ghost slot is clicked or mouse wheeled.
   *
   * @param gui
   *          The GUI the GhostSlot is in
   * @param slot
   *          The GhostSlot
   * @param x
   *          Mouse position x
   * @param y
   *          Mouse position y
   * @param button
   *          The button used (0=left, 1=right). The mouse wheel is mapped to -1=down and -2=up.
   */
  protected void ghostSlotClicked(@Nonnull GuiContainerBase gui, @Nonnull GhostSlot slot, int x, int y, int button) {
    ItemStack handStack = Minecraft.getMinecraft().player.inventory.getItemStack();
    ItemStack existingStack = slot.getStack();
    if (button == 0) { // left
      ghostSlotClickedPrimaryMouseButton(slot, handStack, existingStack);
    } else if (button == 1) { // right
      ghostSlotClickedSecondaryMouseButton(slot, handStack, existingStack);
    } else if (button == -2) { // wheel up
      ghostSlotClickedMouseWheelUp(slot, handStack, existingStack);
    } else if (button == -1) { // wheel down
      ghostSlotClickedMouseWheelDown(slot, handStack, existingStack);
    }
  }

  protected void ghostSlotClickedPrimaryMouseButton(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack, @Nonnull ItemStack existingStack) {
    if (handStack.isEmpty()) { // empty hand
      slot.putStack(ItemStack.EMPTY, 0);
    } else { // item in hand
      if (existingStack.isEmpty()) { // empty slot
        replaceSlot(slot, handStack);
      } else { // filled slot
        if (ItemUtil.areStackMergable(existingStack, handStack)) { // same item
          if (existingStack.getCount() < existingStack.getMaxStackSize() && existingStack.getCount() < slot.getStackSizeLimit()) {
            increaseSlot(slot, existingStack);
          } else {
            // NOP
          }
        } else { // different item
          replaceSlot(slot, handStack);
        }
      }
    }
  }

  protected void ghostSlotClickedSecondaryMouseButton(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack, @Nonnull ItemStack existingStack) {
    if (handStack.isEmpty()) { // empty hand
      slot.putStack(ItemStack.EMPTY, 0);
    } else { // item in hand
      if (existingStack.isEmpty()) { // empty slot
        replaceSlot1Item(slot, handStack);
      } else { // filled slot
        if (ItemUtil.areStackMergable(existingStack, handStack)) { // same item
          descreaseSlot(slot, existingStack);
        } else { // different item
          replaceSlot1Item(slot, handStack);
        }
      }
    }
  }

  protected void ghostSlotClickedMouseWheelUp(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack, @Nonnull ItemStack existingStack) {
    if (!existingStack.isEmpty() && existingStack.getCount() < existingStack.getMaxStackSize() && existingStack.getCount() < slot.getStackSizeLimit()) {
      increaseSlot(slot, existingStack);
    }
  }

  protected void ghostSlotClickedMouseWheelDown(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack, @Nonnull ItemStack existingStack) {
    if (!existingStack.isEmpty()) {
      descreaseSlot(slot, existingStack);
    }
  }

  // Slot interaction tools

  protected void descreaseSlot(@Nonnull GhostSlot slot, @Nonnull ItemStack existingStack) {
    existingStack.shrink(1);
    slot.putStack(existingStack, existingStack.getCount());
  }

  protected void increaseSlot(@Nonnull GhostSlot slot, @Nonnull ItemStack existingStack) {
    existingStack.grow(1);
    slot.putStack(existingStack, existingStack.getCount());
  }

  protected void replaceSlot1Item(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack) {
    ItemStack oneItem = handStack.copy();
    oneItem.setCount(1);
    slot.putStack(oneItem, oneItem.getCount());
  }

  protected void replaceSlot(@Nonnull GhostSlot slot, @Nonnull ItemStack handStack) {
    if (handStack.getCount() <= slot.getStackSizeLimit()) {
      slot.putStack(handStack, handStack.getCount());
    } else {
      ItemStack tmp = handStack.copy();
      tmp.setCount(slot.getStackSizeLimit());
      slot.putStack(tmp, tmp.getCount());
    }
  }

  // Rendering

  protected void startDrawing(@Nonnull GuiContainerBase gui) {
    hoverGhostSlot = null;
  }

  protected void drawGhostSlots(@Nonnull GuiContainerBase gui, int mouseX, int mouseY) {
    int sx = gui.getGuiLeft();
    int sy = gui.getGuiTop();
    gui.drawFakeItemsStart();
    try {
      hoverGhostSlot = null;
      for (GhostSlot slot : ghostSlots) {
        ItemStack stack = slot.getStack();
        if (slot.isVisible()) {
          if (!stack.isEmpty()) {
            gui.drawFakeItemStack(slot.x + sx, slot.y + sy, stack);
            if (slot.shouldDisplayStdOverlay()) {

              gui.drawFakeItemStackStdOverlay(slot.x + sx, slot.y + sy, stack);
            }
            if (slot.shouldGrayOut()) {
              drawGhostSlotGrayout(gui, slot);
            }
          }
          if (slot.isMouseOver(mouseX - sx, mouseY - sy)) {
            hoverGhostSlot = slot;
          }
        }
      }
      final GhostSlot hoverGhostSlot2 = hoverGhostSlot;
      if (hoverGhostSlot2 != null) {
        // draw hover last to prevent it from affecting rendering of other slots ...
        gui.drawFakeItemHover(hoverGhostSlot2.x + sx, hoverGhostSlot2.y + sy);
      }
    } finally {
      gui.drawFakeItemsEnd();
    }
  }

  /**
   * Gray out the item that was just painted into a GhostSlot by over-painting it with 50% transparent background. This gives the illusion that the item was
   * painted with 50% transparency. (100%*a ° 100%*b ° 50%*a == 100%*a ° 50%*b)
   */
  protected void drawGhostSlotGrayout(@Nonnull GuiContainerBase gui, @Nonnull GhostSlot slot) {
    GlStateManager.disableDepth();
    GlStateManager.enableBlend();
    GlStateManager.color(1.0F, 1.0F, 1.0F, slot.getGrayOutLevel());
    String guiTexture = gui.getGuiTexture();
    RenderUtil.bindTexture(guiTexture);
    gui.drawTexturedModalRect(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, slot.x, slot.y, 16, 16);
    GlStateManager.disableBlend();
    GlStateManager.enableDepth();
  }

  protected void drawGhostSlotToolTip(@Nonnull GuiContainerBase gui, int mouseX, int mouseY) {
    final GhostSlot hoverGhostSlot2 = hoverGhostSlot;
    if (hoverGhostSlot2 != null && !gui.mc.player.inventory.getItemStack().isEmpty()) {
      ItemStack stack = hoverGhostSlot2.getStack();
      if (!stack.isEmpty()) {
        gui.renderToolTip(stack, mouseX, mouseY);
      }
    }

  }

}
