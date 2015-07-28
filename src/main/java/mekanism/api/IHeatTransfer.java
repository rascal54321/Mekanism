package mekanism.api;

import net.minecraftforge.common.util.ForgeDirection;

public interface IHeatTransfer
{
	/**The value of the zero point of our temperature scale in kelvin*/
	public static final double AMBIENT_TEMP = 300;

	/**The heat transfer coefficient for air*/
	public static final double AIR_INVERSE_COEFFICIENT = 10000;

	public double getTemp();

	public double getInverseConductionCoefficient();

	public double getInsulationCoefficient(ForgeDirection side);

	public void transferHeatTo(double heat);

	/**Returns the amount of heat expelled from the object to other objects and lost to air*/
	public double[/*2*/] simulateHeat();

	/**Returns the new temperature*/
	public double applyTemperatureChange();

	public boolean canConnectHeat(ForgeDirection side);

	public IHeatTransfer getAdjacent(ForgeDirection side);
}
