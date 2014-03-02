package icbm.sentry.turret.auto;

import universalelectricity.api.vector.Vector3;
import icbm.Reference;
import icbm.sentry.turret.block.TileTurret;
import icbm.sentry.turret.weapon.WeaponProjectile;

/**
 * AA Turret, shoots down missiles and planes.
 * 
 * @author DarkGaurdsman
 */
public class TurretAntiAir extends TurretAuto
{
	public TurretAntiAir(TileTurret host)
	{
		super(host);
		this.weaponSystem = new WeaponProjectile(this, 10)
		{
			@Override
			public void fire(Vector3 target)
			{
				super.fire(target);
				turret.getHost().world().playSoundEffect(turret.getHost().x(), turret.getHost().y(), turret.getHost().z(), Reference.PREFIX + "aagun", 5F, 1F - (turret.getHost().world().rand.nextFloat() * 0.2f));
			}
		};
		this.centerOffset.y = 0.75;
	}
}
