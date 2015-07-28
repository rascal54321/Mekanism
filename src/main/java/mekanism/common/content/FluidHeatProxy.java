package mekanism.common.content;

import mekanism.api.Coord4D;
import mekanism.api.IHeatTransfer;
import mekanism.api.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.util.HeatUtils;
import mekanism.common.util.MekanismUtils;

import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

public class FluidHeatProxy implements IHeatTransfer
{
	public IHeatTransfer parent;
	public FluidStack fluid;
	public ForgeDirection side;

	public FluidHeatProxy(IHeatTransfer from, World w, Coord4D coord, ForgeDirection dir)
	{
		parent = from;
		side = dir.getOpposite();
		fluid = MekanismUtils.getFluid(w, coord, false);
	}

	@Override
	public double getTemp()
	{
		return TemperatureUnit.AMBIENT.convertFromK(fluid.getFluid().getTemperature(fluid));
	}

	@Override
	public double getInverseConductionCoefficient()
	{
		return 1;
	}

	@Override
	public double getInsulationCoefficient(ForgeDirection side)
	{
		return 0;
	}

	@Override
	public void transferHeatTo(double heat)
	{
	}

	@Override
	public double[] simulateHeat()
	{
		return HeatUtils.simulate(this);
	}

	@Override
	public double applyTemperatureChange()
	{
		return getTemp();
	}

	@Override
	public boolean canConnectHeat(ForgeDirection side)
	{
		return true;
	}

	@Override
	public IHeatTransfer getAdjacent(ForgeDirection side)
	{
		return side == this.side ? parent : null;
	}
}
