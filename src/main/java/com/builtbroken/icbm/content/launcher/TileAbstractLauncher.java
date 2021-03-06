package com.builtbroken.icbm.content.launcher;

import com.builtbroken.icbm.ICBM;
import com.builtbroken.icbm.api.launcher.INamedLauncher;
import com.builtbroken.icbm.api.modules.IMissile;
import com.builtbroken.icbm.content.items.ItemRemoteDetonator;
import com.builtbroken.icbm.content.launcher.controller.local.TileLocalController;
import com.builtbroken.icbm.content.launcher.controller.remote.connector.TileCommandSiloConnector;
import com.builtbroken.icbm.content.launcher.controller.remote.display.TileSiloInterface;
import com.builtbroken.icbm.content.launcher.gui.ContainerSilo;
import com.builtbroken.icbm.content.launcher.gui.GuiSiloSettings;
import com.builtbroken.icbm.content.missile.EntityMissile;
import com.builtbroken.icbm.content.missile.tracking.MissileTrackingData;
import com.builtbroken.icbm.content.storage.IMissileMagOutput;
import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.mc.api.event.TriggerCause;
import com.builtbroken.mc.api.items.tools.IWorldPosItem;
import com.builtbroken.mc.api.tile.IFoFProvider;
import com.builtbroken.mc.api.tile.ILinkFeedback;
import com.builtbroken.mc.api.tile.ILinkable;
import com.builtbroken.mc.api.tile.IPassCode;
import com.builtbroken.mc.api.tile.access.IGuiTile;
import com.builtbroken.mc.api.tile.node.ITileNodeHost;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.network.IPacketIDReceiver;
import com.builtbroken.mc.core.network.packet.PacketType;
import com.builtbroken.mc.framework.item.ItemBase;
import com.builtbroken.mc.imp.transform.vector.Location;
import com.builtbroken.mc.imp.transform.vector.Pos;
import com.builtbroken.mc.lib.helper.MathUtility;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Prefab for all missile launchers and silos.
 * Created by robert on 1/18/2015.
 */
public abstract class TileAbstractLauncher extends TileMissileContainer implements INamedLauncher, IPacketIDReceiver, IPassCode, ILinkFeedback, ILinkable, IGuiTile, IMissileMagOutput
{
    /** Current target location */
    public Pos target = new Pos(0, -1, 0);

    public Pos fofStationPos;
    public IFoFProvider fofStation;

    /** Security code used to prevent remote linking */
    protected short link_code;

    /** User customized display name for the launcher */
    protected String customName;

    /** List of launch reports, records what missiles did */
    protected List<LauncherReport> launcherReports = new ArrayList();

    public HashMap<EntityPlayer, Object[]> returnGuiData = new HashMap();

    public TileAbstractLauncher(String id, String mod)
    {
        super(id, mod);
    }

    public void setTarget(Pos target)
    {
        this.target = target;
        sendPacketToServer(getPacketForData(1, target));
    }

    @Override
    public boolean onPlayerActivated(EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IWorldPosItem)
        {
            return false;
        }
        return super.onPlayerActivated(player, side, hitX, hitY, hitZ);
    }

    @Override
    public short getCode()
    {
        if (link_code == 0)
        {
            link_code = MathUtility.randomShort();
        }
        return link_code;
    }

    @Override
    public String link(Location loc, short code)
    {
        //Validate location data
        if (loc.world != world())
        {
            return "link.error.world.match";
        }

        Pos pos = loc.toPos();
        if (!pos.isAboveBedrock())
        {
            return "link.error.pos.invalid";
        }
        if (distance(pos) > TileLocalController.MAX_LINK_DISTANCE)
        {
            return "link.error.pos.distance.max";
        }

        //Compare tile pass code
        TileEntity tile = pos.getTileEntity(loc.world());
        if (tile instanceof IPassCode && ((IPassCode) tile).getCode() != code)
        {
            return "link.error.code.match";
        }
        else if (tile instanceof ITileNodeHost && ((ITileNodeHost) tile).getTileNode() instanceof TileLocalController)
        {
            return ((TileLocalController) ((ITileNodeHost) tile).getTileNode()).link(toLocation(), getCode());
        }
        else if (tile instanceof IFoFProvider)
        {
            IFoFProvider station = getFoFStation();
            if (station == tile)
            {
                return "link.error.tile.already.added";
            }
            else
            {
                fofStation = (IFoFProvider) tile;
                fofStationPos = new Pos(tile);
            }
            return "";
        }
        else
        {
            return "link.error.tile.invalid";
        }
    }

    public IFoFProvider getFoFStation()
    {
        if ((fofStation == null || fofStation instanceof TileEntity && ((TileEntity) fofStation).isInvalid()) && fofStationPos != null)
        {
            TileEntity tile = fofStationPos.getTileEntity(world());
            if (tile instanceof IFoFProvider)
            {
                fofStation = (IFoFProvider) tile;
            }
            else
            {
                fofStationPos = null;
            }
        }
        return fofStation;
    }

    @Override
    public void firstTick()
    {
        if (!target.isAboveBedrock())
        {
            target = new Pos(this);
        }
        if (link_code == 0)
        {
            link_code = MathUtility.randomShort();
        }
    }

    @Override
    public void update(long ticks)
    {
        //TODO track location of missiles if enabled
        //TODO track ETA to target of missiles if enabled
        super.update(ticks);
        if (isServer())
        {
            //TODO do count down rather than every 1 second
            if (ticks % 20 == 0)
            {
                if (world().isBlockIndirectlyGettingPowered(xi(), yi(), zi()))
                {
                    if (fireMissile(target))
                    {
                        //TODO confirm missile launch to controllers?
                    }
                    else
                    {
                        //TODO make error effect or noise?
                    }
                }
            }
        }
    }

    @Override
    public void doCleanupCheck()
    {
        //Cleans up the report list looking for broken reports, temp fix for NULL UUIDs
        List<LauncherReport> newList = new ArrayList();
        for (LauncherReport report : launcherReports)
        {
            if (report.entityUUID != null && report.missile != null)
            {
                newList.add(report);
            }
        }
        launcherReports.clear();
        launcherReports = newList;
    }

    /**
     * Can the missile be fired from the silo. Does not
     * and should not check if a missile is functional,
     * completed, or contained in the silo. This is
     * only for logic controlling of the missile. Allowing
     * for fails safes to be added such as detecting if
     * missile doors are opened.
     *
     * @return true if the missile should fire.
     */
    public boolean canFireMissile()
    {
        return true;
    }

    @Override
    public boolean fireMissile()
    {
        return fireMissile(target);
    }

    @Override
    public boolean fireMissile(IPos3D target)
    {
        if (canFireMissile())
        {
            //We have a missile?
            IMissile missile = getMissile();
            if (missile != null)
            {
                //Does it have an engine
                if (missile.canLaunch())
                {
                    if (isServer())
                    {

                        //Create and setup missile
                        EntityMissile entity = new EntityMissile(world());
                        entity.setMissile(missile);

                        ICBM.INSTANCE.logger().info("Firing missile from " + this + ", Missile = " + entity + ", Target = " + target);

                        if (fofStation != null)
                        {
                            entity.fofTag = fofStation.getProvidedFoFTag();
                        }

                        //Set location data
                        Pos start = new Pos(this).add(getMissileLaunchOffset());
                        entity.setPositionAndRotation(start.x(), start.y(), start.z(), 0, 0);
                        entity.motionY = missile.getEngine().getSpeed(missile);

                        //Set target data
                        entity.setTarget(target, true);
                        entity.sourceOfProjectile = new Pos(this);

                        //Spawn and start moving
                        world().spawnEntityInWorld(entity);
                        addLaunchReport(entity);

                        entity.setIntoMotion();

                        //Empty inventory slot
                        getInventory().setInventorySlotContents(0, null);
                        onPostMissileFired(target instanceof Pos ? (Pos) target : new Pos(target), entity);
                    }
                    else
                    {
                        triggerLaunchingEffects();
                    }
                    return true;
                }
                //No engine can result in warhead detonating due to ignition source shooting up into warhead cavity
                else if (isServer() && missile.getEngine() == null && world().rand.nextFloat() > 0.9f)
                {
                    //If the user is stupid enough to not install an engine....
                    if (missile.getWarhead() != null)
                    {
                        missile.getWarhead().trigger(new TriggerCause.TriggerCauseFire(ForgeDirection.DOWN), world(), xi(), yi(), zi());
                        getInventory().setInventorySlotContents(0, null);
                    }
                    else
                    {
                        //Location pos = toLocation().add(getMissileLaunchOffset());
                        //ExplosiveRegistry.triggerExplosive(pos, ExplosiveRegistry.get("TNT"), new TriggerCause.TriggerCauseFire(ForgeDirection.DOWN), 2, null);
                        //TODO set fire to missile damaging components
                    }
                }
            }
        }
        return false;
    }

    /**
     * Called after the missile has been launched
     *
     * @param target
     * @param entity
     */
    protected void onPostMissileFired(final Pos target, EntityMissile entity)
    {
        if (isServer())
        {
            sendDescPacket();
        }
    }

    /**
     * Called to add a launch report for a missile.
     *
     * @param missile - missile to generate a report for.
     */
    protected void addLaunchReport(EntityMissile missile)
    {
        launcherReports.add(new LauncherReport(missile));
        if (launcherReports.size() > 20)
        {
            launcherReports.remove(0);
        }
    }

    /**
     * Called to ensure the missile doesn't clip the edge of a multi-block
     * structure that holds the missile.
     *
     * @return Position in relation to the launcher base, do not add location data
     */
    public Pos getMissileLaunchOffset()
    {
        return new Pos(0, 3, 0);
    }

    /**
     * Called to load up and populate some effects in addition to the missile's own
     * launching effects.
     */
    public void triggerLaunchingEffects()
    {
        //TODO add more effects
        for (int l = 0; l < 20; ++l)
        {
            double f = x() + 0.5 + 0.3 * (world().rand.nextFloat() - world().rand.nextFloat());
            double f1 = y() + 0.1 + 0.5 * (world().rand.nextFloat() - world().rand.nextFloat());
            double f2 = z() + 0.5 + 0.3 * (world().rand.nextFloat() - world().rand.nextFloat());
            world().spawnParticle("largesmoke", f, f1, f2, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * Called when the missile fired from this launcher impacts
     * the ground. Used for tracking information and feed back
     * for users.
     *
     * @param missile - entity fired by this launcher
     */
    public void onImpactOfMissile(EntityMissile missile)
    {
        if (isServer() && missile != null)
        {
            //TODO thin out list to only include active reports, or rather ones still waiting on death times to be added
            for (LauncherReport report : launcherReports)
            {
                if (report.entityUUID != null && report.entityUUID.getMostSignificantBits() == missile.getUniqueID().getMostSignificantBits())
                {
                    report.impacted = true;
                    break;
                }
            }
        }
    }

    /**
     * Reports the death of the missile to the silo.
     *
     * @param missile - missile that died, no reason given.
     */
    public void onDeathOfMissile(EntityMissile missile)
    {
        if (isServer() && missile != null)
        {
            for (LauncherReport report : launcherReports)
            {
                if (report.entityUUID != null && report.entityUUID.getMostSignificantBits() == missile.getUniqueID().getMostSignificantBits())
                {
                    report.deathTime = System.nanoTime();
                    break;
                }
            }
        }
    }

    @Override
    public void onLinked(Location location)
    {

    }

    @Override
    public boolean read(ByteBuf buf, int id, EntityPlayer player, PacketType type)
    {
        if (id == 1)
        {
            this.target = new Pos(buf);
            return true;
        }
        else if (id == 22)
        {
            this.customName = ByteBufUtils.readUTF8String(buf);
            return true;
        }
        else if (isServer())
        {
            if (id == 23)
            {
                ItemStack stack = ((ContainerSilo) player.openContainer).basicInventory.getStackInSlot(0);
                if (stack != null && stack.getItem() instanceof ItemBase && ((ItemBase) stack.getItem()).node instanceof ItemRemoteDetonator)
                {
                    if (returnGuiData.containsKey(player) && player.openContainer instanceof ContainerSilo)
                    {
                        if (returnGuiData.get(player)[1] instanceof TileCommandSiloConnector && returnGuiData.get(player)[0] instanceof TileSiloInterface)
                        {
                            TileCommandSiloConnector connector = (TileCommandSiloConnector) returnGuiData.get(player)[1];
                            TileSiloInterface tileSiloInterface = (TileSiloInterface) returnGuiData.get(player)[0];
                            if (tileSiloInterface.getCommandCenter() != null && tileSiloInterface.getCommandCenter().getAttachedNetworks().size() > 0 && connector.getConnectorGroupName() != null && getCustomName() != null)
                            {
                                ((ItemRemoteDetonator) ((ItemBase) stack.getItem()).node).encode(stack, tileSiloInterface.getCommandCenter().getAttachedNetworks().get(0).getHz(), link_code, connector.getConnectorGroupName(), getCustomName());
                            }
                            else if (player instanceof EntityPlayerMP)
                            {
                                if (tileSiloInterface == null || tileSiloInterface.getCommandCenter().getAttachedNetworks().size() <= 0)
                                {
                                    Engine.instance.packetHandler.sendToPlayer(getPacketForData(24, "error.data.missing.hz"), (EntityPlayerMP) player);
                                }
                                else if (connector.getConnectorGroupName() == null)
                                {
                                    Engine.instance.packetHandler.sendToPlayer(getPacketForData(24, "error.data.missing.groupName"), (EntityPlayerMP) player);
                                }
                                else if (getCustomName() == null)
                                {
                                    Engine.instance.packetHandler.sendToPlayer(getPacketForData(24, "error.data.missing.siloName"), (EntityPlayerMP) player);
                                }
                                else
                                {
                                    Engine.instance.packetHandler.sendToPlayer(getPacketForData(24, "error.data.missing"), (EntityPlayerMP) player);
                                }
                            }
                        }
                        else if (player instanceof EntityPlayerMP)
                        {
                            Engine.instance.packetHandler.sendToPlayer(getPacketForData(24, "error.invalid.connection"), (EntityPlayerMP) player);
                        }
                    }
                }
                return true;
            }
            else if (id == 25)
            {
                if (returnGuiData.containsKey(player) && player.openContainer instanceof ContainerSilo)
                {
                    Object tile = returnGuiData.get(player)[0];
                    if (tile instanceof IGuiTile)
                    {
                        ((IGuiTile) tile).openGui(player, player.openContainer, returnGuiData.get(player));
                    }
                }
                return true;
            }
        }
        else if (isClient())
        {
            if (id == 24)
            {
                if (Minecraft.getMinecraft().currentScreen instanceof GuiSiloSettings)
                {
                    ((GuiSiloSettings) Minecraft.getMinecraft().currentScreen).errorString = ByteBufUtils.readUTF8String(buf);
                }
                return true;
            }
        }
        return super.read(buf, id, player, type);
    }

    /**
     * Sets a custom name for the launcher. Never call from packet
     * handling as this auto sends packets. You will create
     * a packet infinite loop. That can not be stopped.
     *
     * @param name - valid name
     */
    public void setCustomName(String name)
    {
        this.customName = name;
        if (isClient())
        {
            sendPacketToServer(getPacketForData(22, name));
        }
        else
        {
            sendDescPacket();
        }
    }

    public String getCustomName()
    {
        return customName;
    }

    @Override
    public void readDescPacket(ByteBuf buf)
    {
        super.readDescPacket(buf);
        target = new Pos(buf);
        customName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void writeDescPacket(ByteBuf buf)
    {
        super.writeDescPacket(buf);
        target.writeByteBuf(buf);
        ByteBufUtils.writeUTF8String(buf, "" + customName);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt)
    {
        super.save(nbt);
        if (nbt.hasKey("target"))
        {
            this.target = new Pos(nbt.getCompoundTag("target"));
        }
        if (nbt.hasKey("link_code"))
        {
            this.link_code = nbt.getShort("link_code");
        }
        else
        {
            this.link_code = (short) MathUtility.rand.nextInt(Short.MAX_VALUE);
        }

        if (nbt.hasKey("launchReports"))
        {
            //Clear list to remove duplication
            launcherReports.clear();

            NBTTagList list = nbt.getTagList("launchReports", 10);
            for (int i = 0; i < list.tagCount(); i++)
            {
                launcherReports.add(new LauncherReport(list.getCompoundTagAt(i)));
            }
        }

        if (nbt.hasKey("fofStationPos"))
        {
            fofStationPos = new Pos(nbt.getCompoundTag("fofStationPos"));
        }

        if (nbt.hasKey("customName"))
        {
            customName = nbt.getString("customName");
        }
        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt)
    {
        super.load(nbt);
        if (target != null)
        {
            nbt.setTag("target", target.toNBT());
        }
        if (link_code != 0)
        {
            nbt.setShort("link_code", link_code);
        }

        if (launcherReports != null && launcherReports.size() > 0)
        {
            NBTTagList list = new NBTTagList();
            for (LauncherReport report : launcherReports)
            {
                list.appendTag(report.save());
            }
            nbt.setTag("launchReports", list);
        }

        if (fofStationPos != null)
        {
            nbt.setTag("fofStationPos", fofStationPos.toNBT());
        }

        if (customName != null && !customName.isEmpty())
        {
            nbt.setString("customName", customName);
        }
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player)
    {
        if (id == 1)
        {
            return new ContainerSilo(player, this, true);
        }
        else if (id == 2)
        {
            return new ContainerSilo(player, this, false);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player)
    {
        if (id == 1)
        {
            return new GuiSiloSettings(this, player, true);
        }
        else if (id == 2)
        {
            return new GuiSiloSettings(this, player, false);
        }
        return null;
    }

    public void encodeItem(EntityPlayer player)
    {
        //TODO auth player
        sendPacketToServer(getPacketForData(23));
    }

    public void returnToPrevGui()
    {
        sendPacketToServer(getPacketForData(25));
    }

    @Override
    public IPos3D getTarget()
    {
        return target;
    }

    @Override
    public int getTravelTimeTo(IPos3D target)
    {
        IMissile missile = getMissile();
        return missile == null ? -1 : (int) MissileTrackingData.getRespawnTicks(toPos(), new Pos(getTarget()), missile.getEngine() != null ? missile.getEngine().getSpeed(missile) : 1);
    }

    @Override
    public String getLauncherName()
    {
        return getCustomName();
    }
}
