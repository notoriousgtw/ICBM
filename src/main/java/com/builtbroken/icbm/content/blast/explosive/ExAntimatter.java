package com.builtbroken.icbm.content.blast.explosive;

import com.builtbroken.icbm.api.missile.IMissileEntity;
import com.builtbroken.icbm.api.modules.IMissile;
import com.builtbroken.icbm.api.modules.IWarhead;
import com.builtbroken.icbm.content.blast.ExplosiveHandlerICBM;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 1/30/2016.
 */
public class ExAntimatter extends ExplosiveHandlerICBM<BlastAntimatter>
{
    /**
     * Creates an explosive using a blast class, and name
     */
    public ExAntimatter()
    {
        super("Antimatter", 1);
    }

    @Override
    protected BlastAntimatter newBlast()
    {
        return new BlastAntimatter(this);
    }

    @Override
    public boolean doesVaporizeParts(IMissileEntity entity, IMissile missile, IWarhead warhead, boolean warheadBlew, boolean engineBlew)
    {
        return true;
    }
}
