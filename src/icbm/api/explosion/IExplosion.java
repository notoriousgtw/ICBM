package icbm.api.explosion;

/**
 * The actual explosion interface. Extends Explosion.java.
 * 
 * @author Calclavia
 * 
 */
public interface IExplosion
{
	/**
	 * Called to initiate the explosion.
	 */
	public void explode();

	/**
	 * @return The radius of effect of the explosion.
	 */
	public float getRadius();

	/**
	 * @return The energy emitted by this explosive. In Joules and approximately based off of a real
	 * life equivalent.
	 */
	public float getEnergy();

	/**
	 * Called before an explosion happens.
	 */
	public void preExplode();

	/**
	 * Called every tick when this explosive is being progressed.
	 */
	public void onExplode();

	/**
	 * Called after the explosion is completed.
	 */
	public void postExplode();
}
