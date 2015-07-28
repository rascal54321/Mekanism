package mekanism.common.tile;

import mekanism.api.Coord4D;
import mekanism.api.IHeatTransfer;
import mekanism.common.content.FluidHeatProxy;
import mekanism.common.util.HeatUtils;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityRadiator extends TileEntityBasicBlock implements IHeatTransfer
{
	public double temperature;
	public double heatToAbsorb;

	public double inverseHeatCapacity = 1;

	public IHeatTransfer[] cachedHeatDevices = new IHeatTransfer[6];

	@Override
	public void onUpdate()
	{
		simulateHeat();
		applyTemperatureChange();
	}

	@Override
	public void onNeighborChange(Block block)
	{
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			Coord4D sideCoord = Coord4D.get(this).getFromSide(side);
			TileEntity sideTile = sideCoord.getTileEntity(getWorldObj());
			if(sideTile instanceof IHeatTransfer)
			{
				cachedHeatDevices[side.ordinal()] = (IHeatTransfer)sideTile;
				continue;
			}
			else
			{
				FluidHeatProxy proxy = new FluidHeatProxy(this, getWorldObj(), sideCoord, side);
				cachedHeatDevices[side.ordinal()] = (proxy.fluid == null ? null : proxy);
			}
		}
	}

	@Override
	public double getTemp()
	{
		return temperature;
	}

	@Override
	public double getInverseConductionCoefficient()
	{
		return 5;
	}

	@Override
	public double getInsulationCoefficient(ForgeDirection side)
	{
		return 0;
	}

	@Override
	public void transferHeatTo(double heat)
	{
		heatToAbsorb += heat;
	}

	@Override
	public double[] simulateHeat()
	{
		for(IHeatTransfer transfer : cachedHeatDevices)
		{
			if(transfer instanceof FluidHeatProxy)
			{
				transfer.simulateHeat();
			}
		}
		return HeatUtils.simulate(this);
	}

	@Override
	public double applyTemperatureChange()
	{
		temperature += inverseHeatCapacity * heatToAbsorb;
		heatToAbsorb = 0;
		return temperature;
	}

	@Override
	public boolean canConnectHeat(ForgeDirection side)
	{
		return true;
	}

	@Override
	public IHeatTransfer getAdjacent(ForgeDirection side)
	{
		return cachedHeatDevices[side.ordinal()];
	}
}
