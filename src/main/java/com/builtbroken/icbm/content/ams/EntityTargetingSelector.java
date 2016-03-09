package com.builtbroken.icbm.content.ams;

import com.builtbroken.icbm.api.missile.IFoF;
import com.builtbroken.icbm.api.missile.IMissileEntity;
import com.builtbroken.mc.api.ISave;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Targeting filter used to select entities near an AMS system
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/5/2016.
 */
public class EntityTargetingSelector implements IEntitySelector, ISave
{
    final TileAMS ams;

    public EntityTargetingSelector(TileAMS ams)
    {
        this.ams = ams;
    }

    @Override
    public boolean isEntityApplicable(Entity entity)
    {
        //TODO ray trace targets
        if (entity instanceof IMissileEntity)
        {
            if (entity instanceof IFoF && ams.fofStation != null)
            {
                if (((IFoF) entity).getFoFTag() == ams.fofStation.getProvidedFoFTag())
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void load(NBTTagCompound nbt)
    {

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt)
    {
        return nbt;
    }
}
