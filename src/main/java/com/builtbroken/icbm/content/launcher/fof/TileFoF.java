package com.builtbroken.icbm.content.launcher.fof;

import com.builtbroken.icbm.api.missile.IFoF;
import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.jlib.helpers.MathHelper;
import com.builtbroken.jlib.lang.EnglishLetters;
import com.builtbroken.mc.api.tile.IGuiTile;
import com.builtbroken.mc.api.tile.multiblock.IMultiTile;
import com.builtbroken.mc.api.tile.multiblock.IMultiTileHost;
import com.builtbroken.mc.lib.transform.vector.Location;
import com.builtbroken.mc.lib.transform.vector.Pos;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.prefab.tile.TileModuleMachine;
import com.builtbroken.mc.prefab.tile.multiblock.EnumMultiblock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.HashMap;

/**
 * Friend or foe controller, used to sync FoF tags between launchers, AMS, and other tiles.
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/9/2016.
 */
public class TileFoF extends TileModuleMachine implements IGuiTile, IMultiTileHost, IFoFStation
{
    private static final HashMap<IPos3D, String> STRUCTURE = new HashMap();

    static
    {
        STRUCTURE.put(new Pos(0, 1, 0), EnumMultiblock.TILE.getName());
    }

    /** Main ID used for FoF system */
    protected String userFoFID;

    private boolean breaking = false;

    public TileFoF()
    {
        super("ICBMxFoF", Material.iron);
        this.hardness = 15f;
        this.resistance = 50f;
        //this.renderNormalBlock = false;
        this.addInventoryModule(2);
    }

    @Override
    public void firstTick()
    {
        super.firstTick();
        if (isServer())
        {
            if (userFoFID == null || userFoFID.isEmpty())
            {
                userFoFID = "";
                //Generate random default string
                int[] l = MathHelper.generateRandomIntArray(world().rand, EnglishLetters.values().length - 1 + 10, 10 + world().rand.nextInt(20));
                for (int i : l)
                {
                    if (i <= 10)
                    {
                        userFoFID += i - 1;
                    }
                    else if (world().rand.nextBoolean())
                    {
                        userFoFID += EnglishLetters.values()[i - 10].name();
                    }
                    else
                    {
                        userFoFID += EnglishLetters.values()[i - 10].name().toLowerCase();
                    }
                }
            }
        }
    }

    @Override
    public void update()
    {
        super.update();
    }


    @Override
    public String getProvidedFoFTag()
    {
        return userFoFID;
    }

    @Override
    public boolean isFriendly(Entity entity)
    {
        if (entity instanceof IFoF)
        {
            return ((IFoF) entity).getFoFTag() != null && ((IFoF) entity).getFoFTag().equals(getProvidedFoFTag());
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        if (nbt.hasKey("fofID"))
        {
            userFoFID = nbt.getString("fofID");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        if (userFoFID != null && !userFoFID.isEmpty())
        {
            nbt.setString("fofID", userFoFID);
        }
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player)
    {
        return new ContainerFoF(player, this);
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player)
    {
        return null;
    }

    @Override
    public void onMultiTileAdded(IMultiTile tileMulti)
    {

    }

    @Override
    public boolean onMultiTileBroken(IMultiTile tileMulti, Object source, boolean harvest)
    {
        if (tileMulti instanceof TileEntity && ((TileEntity) tileMulti).xCoord == xCoord && ((TileEntity) tileMulti).yCoord == (yCoord + 1) && ((TileEntity) tileMulti).zCoord == zCoord)
        {
            breaking = true;
            Location loc = toLocation();
            if (harvest)
            {
                InventoryUtility.dropItemStack(loc, toItemStack());
            }
            loc.setBlockToAir();
            breaking = false;
        }
        return false;
    }

    @Override
    public void onRemove(Block block, int par6)
    {
        breaking = true;
        world().setBlockToAir(xCoord, yCoord + 1, zCoord);
        breaking = false;
    }

    @Override
    public void onTileInvalidate(IMultiTile tileMulti)
    {

    }

    @Override
    public boolean onMultiTileActivated(IMultiTile tile, EntityPlayer player, int side, IPos3D hit)
    {
        return onPlayerActivated(player, side, hit instanceof Pos ? (Pos) hit : new Pos(hit));
    }

    @Override
    public void onMultiTileClicked(IMultiTile tile, EntityPlayer player)
    {

    }

    @Override
    public HashMap<IPos3D, String> getLayoutOfMultiBlock()
    {
        return STRUCTURE;
    }
}
