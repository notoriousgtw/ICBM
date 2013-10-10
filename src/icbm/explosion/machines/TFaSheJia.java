package icbm.explosion.machines;

import icbm.api.ITier;
import icbm.explosion.ICBMExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;
import universalelectricity.prefab.tile.IRotatable;
import universalelectricity.prefab.tile.TileEntityAdvanced;
import calclavia.lib.multiblock.IMultiBlock;

import com.google.common.io.ByteArrayDataInput;

/**
 * This tile entity is for the screen of the missile launcher
 * 
 * @author Calclavia
 * 
 */
public class TFaSheJia extends TileEntityAdvanced implements IPacketReceiver, ITier, IMultiBlock, IRotatable
{
	// The tier of this screen
	private int tier = 0;

	private byte orientation = 3;

	@Override
	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try
		{
			this.orientation = dataStream.readByte();
			this.tier = dataStream.readInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public Packet getDescriptionPacket()
	{
		return PacketManager.getPacket(ICBMExplosion.CHANNEL, this, this.orientation, this.getTier());
	}

	/**
	 * Gets the inaccuracy of the missile based on the launcher support frame's tier
	 */
	public int getInaccuracy()
	{
		switch (this.tier)
		{
			default:
				return 15;
			case 1:
				return 7;
			case 2:
				return 0;
		}
	}

	/**
	 * Determines if this TileEntity requires update calls.
	 * 
	 * @return True if you want updateEntity() to be called, false if not
	 */
	@Override
	public boolean canUpdate()
	{
		return false;
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readFromNBT(par1NBTTagCompound);
		this.tier = par1NBTTagCompound.getInteger("tier");
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setInteger("tier", this.tier);
	}

	@Override
	public int getTier()
	{
		return this.tier;
	}

	@Override
	public void setTier(int tier)
	{
		this.tier = tier;
	}

	@Override
	public Vector3[] getMultiBlockVectors()
	{
		return new Vector3[] { new Vector3(0, 1, 0), new Vector3(0, 2, 0) };
	}

	@Override
	public ForgeDirection getDirection()
	{
		return ForgeDirection.getOrientation(this.orientation);
	}

	@Override
	public void setDirection(ForgeDirection facingDirection)
	{
		this.orientation = (byte) facingDirection.ordinal();
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}
}