package mekanism.generators.common.tile.turbine;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.Mekanism;
import mekanism.common.multiblock.MultiblockCache;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.multiblock.UpdateProtocol;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityMultiblock;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.content.turbine.SynchronizedTurbineData;
import mekanism.generators.common.content.turbine.TurbineCache;
import mekanism.generators.common.content.turbine.TurbineUpdateProtocol;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityTurbineCasing extends TileEntityMultiblock<SynchronizedTurbineData> implements IStrictEnergyStorage
{
	public static final double DISPERSER_GAS_FLOW = 640;
	public static final double VENT_GAS_FLOW = 16000;
	public static final double ENERGY_PER_STEAM = 10;
	public static final double BLADE_TO_COIL_RATIO = 4;
	
	public float prevScale;
	
	public TileEntityTurbineCasing() 
	{
		this("TurbineCasing");
	}
	
	public TileEntityTurbineCasing(String name)
	{
		super(name);
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(worldObj.isRemote)
		{
			if(structure != null && clientHasStructure && isRendering)
			{
				float targetScale = (float)(structure.fluidStored != null ? structure.fluidStored.amount : 0)/structure.getFluidCapacity();

				if(Math.abs(prevScale - targetScale) > 0.01)
				{
					prevScale = (9*prevScale + targetScale)/10;
				}
			}
		}

		if(!worldObj.isRemote)
		{
			if(structure != null)
			{
				structure.lastSteamInput = 0;
				
				if(structure.fluidStored != null && structure.fluidStored.amount <= 0)
				{
					structure.fluidStored = null;
					markDirty();
				}
				
				if(isRendering)
				{
					int stored = structure.fluidStored != null ? structure.fluidStored.amount : 0;
					double proportion = (double)stored/(double)structure.getFluidCapacity();
					double flowRate = 0;
					
					if(stored > 0 && structure.electricityStored < structure.getEnergyCapacity())
					{
						double energyMultiplier = ENERGY_PER_STEAM*Math.min(structure.blades, structure.coils*BLADE_TO_COIL_RATIO);
						double rate = structure.lowerVolume*(structure.getDispersers()*DISPERSER_GAS_FLOW)*proportion;
						double origRate = rate;
						
						rate = Math.min(Math.min(stored, rate), structure.vents*VENT_GAS_FLOW);
						rate = Math.min(rate, (getMaxEnergy()-getEnergy())/energyMultiplier);
						
						flowRate = proportion*(rate/origRate);
						setEnergy(getEnergy()+((int)rate)*energyMultiplier);
						
						structure.fluidStored.amount -= rate;
						structure.clientFlow = (int)rate;
						
						if(structure.fluidStored.amount == 0)
						{
							structure.fluidStored = null;
						}
					}
					else {
						structure.clientFlow = 0;
					}
					
					TileEntity tile = structure.complex.getTileEntity(worldObj);
					
					if(tile instanceof TileEntityRotationalComplex)
					{
						((TileEntityRotationalComplex)tile).setRotation((float)flowRate);
					}
					
					if(structure.needsRenderUpdate())
					{
						sendPacketToRenderer();
					}
					
					structure.prevFluid = structure.fluidStored;
				}
			}
		}
	}
	
	@Override
	public boolean onActivate(EntityPlayer player)
	{
		if(!player.isSneaking() && structure != null)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
			player.openGui(MekanismGenerators.instance, 6, worldObj, xCoord, yCoord, zCoord);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public double getEnergy()
	{
		return structure != null ? structure.electricityStored : 0;
	}
	
	@Override
	public double getMaxEnergy() 
	{
		return structure.getEnergyCapacity();
	}

	@Override
	public void setEnergy(double energy)
	{
		if(structure != null)
		{
			structure.electricityStored = Math.max(Math.min(energy, getMaxEnergy()), 0);
			MekanismUtils.saveChunk(this);
		}
	}
	
	public int getScaledFluidLevel(int i)
	{
		if(structure.getFluidCapacity() == 0 || structure.fluidStored == null)
		{
			return 0;
		}

		return structure.fluidStored.amount*i / structure.getFluidCapacity();
	}
	
	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);
		
		if(structure != null)
		{
			data.add(structure.volume);
			data.add(structure.lowerVolume);
			data.add(structure.vents);
			data.add(structure.blades);
			data.add(structure.coils);
			data.add(structure.getDispersers());
			data.add(structure.electricityStored);
			data.add(structure.clientFlow);
			data.add(structure.lastSteamInput);
			
			if(structure.fluidStored != null)
			{
				data.add(1);
				data.add(structure.fluidStored.getFluidID());
				data.add(structure.fluidStored.amount);
			}
			else {
				data.add(0);
			}
		}

		return data;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);
		
		if(clientHasStructure)
		{
			structure.volume = dataStream.readInt();
			structure.lowerVolume = dataStream.readInt();
			structure.vents = dataStream.readInt();
			structure.blades = dataStream.readInt();
			structure.coils = dataStream.readInt();
			structure.clientDispersers = dataStream.readInt();
			structure.electricityStored = dataStream.readDouble();
			structure.clientFlow = dataStream.readInt();
			structure.lastSteamInput = dataStream.readInt();
			
			if(dataStream.readInt() == 1)
			{
				structure.fluidStored = new FluidStack(FluidRegistry.getFluid(dataStream.readInt()), dataStream.readInt());
			}
			else {
				structure.fluidStored = null;
			}
		}
	}

	@Override
	protected SynchronizedTurbineData getNewStructure() 
	{
		return new SynchronizedTurbineData();
	}

	@Override
	public MultiblockCache<SynchronizedTurbineData> getNewCache() 
	{
		return new TurbineCache();
	}

	@Override
	protected UpdateProtocol<SynchronizedTurbineData> getProtocol() 
	{
		return new TurbineUpdateProtocol(this);
	}

	@Override
	public MultiblockManager<SynchronizedTurbineData> getManager() 
	{
		return MekanismGenerators.turbineManager;
	}
}