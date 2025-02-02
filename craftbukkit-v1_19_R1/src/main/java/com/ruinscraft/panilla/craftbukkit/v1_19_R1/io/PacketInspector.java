package com.ruinscraft.panilla.craftbukkit.v1_19_R1.io;

import com.ruinscraft.panilla.api.IPanilla;
import com.ruinscraft.panilla.api.IPanillaPlayer;
import com.ruinscraft.panilla.api.exception.EntityNbtNotPermittedException;
import com.ruinscraft.panilla.api.exception.FailedNbt;
import com.ruinscraft.panilla.api.exception.NbtNotPermittedException;
import com.ruinscraft.panilla.api.io.IPacketInspector;
import com.ruinscraft.panilla.api.nbt.INbtTagCompound;
import com.ruinscraft.panilla.api.nbt.checks.NbtChecks;
import com.ruinscraft.panilla.craftbukkit.v1_19_R1.nbt.NbtTagCompound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.protocol.game.PacketPlayOutWindowItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

public class PacketInspector implements IPacketInspector {

    private final IPanilla panilla;

    public PacketInspector(IPanilla panilla) {
        this.panilla = panilla;
    }

    @Override
    public void checkPacketPlayInSetCreativeSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayInSetCreativeSlot) {
            PacketPlayInSetCreativeSlot packet = (PacketPlayInSetCreativeSlot) _packet;

            int slot = packet.b();
            ItemStack itemStack = packet.c();

            if (itemStack == null || !itemStack.t()) return;

            NbtTagCompound tag = new NbtTagCompound(itemStack.v());
            String itemClass = itemStack.c().getClass().getSimpleName();
            String packetClass = packet.getClass().getSimpleName();

            NbtChecks.checkPacketPlayIn(slot, tag, itemClass, packetClass, panilla);
        }
    }

    @Override
    public void checkPacketPlayOutSetSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSetSlot) {
            PacketPlayOutSetSlot packet = (PacketPlayOutSetSlot) _packet;

            int windowId = packet.b();

            // check if window is not player inventory and we are ignoring non-player inventories
            if (windowId != 0 && panilla.getPConfig().ignoreNonPlayerInventories) {
                return;
            }

            int slot = packet.c();

            ItemStack itemStack = packet.d();

            if (itemStack == null || !itemStack.t()) {
                return;
            }

            NbtTagCompound tag = new NbtTagCompound(itemStack.v());
            String itemClass = itemStack.getClass().getSimpleName();
            String packetClass = packet.getClass().getSimpleName();

            NbtChecks.checkPacketPlayOut(slot, tag, itemClass, packetClass, panilla);
        }
    }

    @Override
    public void checkPacketPlayOutWindowItems(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayOutWindowItems) {
            PacketPlayOutWindowItems packet = (PacketPlayOutWindowItems) _packet;

            int windowId = packet.b();

            // check if window is not player inventory
            if (windowId != 0) {
                return;
            }

            List<ItemStack> itemStacks = packet.c();

            for (ItemStack itemStack : itemStacks) {
                if (itemStack == null || !itemStack.t()) {
                    continue;
                }

                NbtTagCompound tag = new NbtTagCompound(itemStack.v());
                String itemClass = itemStack.getClass().getSimpleName();
                String packetClass = packet.getClass().getSimpleName();

                NbtChecks.checkPacketPlayOut(0, tag, itemClass, packetClass, panilla); // TODO: set slot?
            }
        }
    }

    @Override
    public void checkPacketPlayOutSpawnEntity(Object _packet) throws EntityNbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSpawnEntity) {
            PacketPlayOutSpawnEntity packet = (PacketPlayOutSpawnEntity) _packet;

            UUID entityId = packet.c();
            Entity entity = null;

            for (WorldServer worldServer : MinecraftServer.getServer().E()) {
                entity = worldServer.P.d().a(entityId);
                if (entity != null) break;
            }

            if (entity != null) {
                if (entity instanceof EntityItem) {
                    EntityItem item = (EntityItem) entity;

                    if (item.h() == null) {
                        return;
                    }

                    if (!item.h().t()) {
                        return;
                    }

                    INbtTagCompound tag = new NbtTagCompound(item.h().v());
                    String itemName = item.h().c().a();
                    FailedNbt failedNbt = NbtChecks.checkAll(tag, itemName, panilla);

                    if (FailedNbt.fails(failedNbt)) {
                        throw new EntityNbtNotPermittedException(packet.getClass().getSimpleName(), false, failedNbt, entityId, entity.W().getWorld().getName());
                    }
                }
            }
        }
    }

    @Override
    public void sendPacketPlayOutSetSlotAir(IPanillaPlayer player, int slot) {
        CraftPlayer craftPlayer = (CraftPlayer) player.getHandle();
        EntityPlayer entityPlayer = craftPlayer.getHandle();

        try {
            Class<?> packetPlayOutSetSlotClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSetSlot");
            Class<?>[] type = { int.class, int.class, int.class, ItemStack.class };
            Constructor<?> constructor = packetPlayOutSetSlotClass.getConstructor(type);
            Object[] params = { 0, 0, slot, new ItemStack(Blocks.a) };
            Object packetPlayOutSetSlotInstance = constructor.newInstance(params);
            entityPlayer.b.a((Packet<?>) packetPlayOutSetSlotInstance);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stripNbtFromItemEntity(UUID entityId) {
        Entity entity = null;

        for (WorldServer worldServer : MinecraftServer.getServer().E()) {
            entity = worldServer.P.d().a(entityId);
            if (entity != null) break;
        }

        if (entity instanceof EntityItem) {
            EntityItem item = (EntityItem) entity;
            if (item.h() == null) return;
            if (!item.h().t()) return;
            item.h().c((NBTTagCompound) null);
        }
    }

    @Override
    public void stripNbtFromItemEntityLegacy(int entityId) {
        throw new RuntimeException("cannot use #stripNbtFromItemEntityLegacy on 1.19");
    }

    @Override
    public void validateBaseComponentParse(String string) throws Exception {
        IChatBaseComponent.ChatSerializer.a(string);
    }

}
