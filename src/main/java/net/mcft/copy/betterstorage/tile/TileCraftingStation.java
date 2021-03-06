package net.mcft.copy.betterstorage.tile;

import net.mcft.copy.betterstorage.BetterStorage;
import net.mcft.copy.betterstorage.config.GlobalConfig;
import net.mcft.copy.betterstorage.misc.Constants;
import net.mcft.copy.betterstorage.tile.entity.TileEntityCraftingStation;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileCraftingStation extends TileContainerBetterStorage {
	
	private Icon topIcon;
	private Icon bottomIconDisabled;
	private Icon bottomIconEnabled;
	
	public TileCraftingStation(int id) {
		super(id, Material.iron);
		
		setHardness(1.5f);
		setStepSound(soundStoneFootstep);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {
		blockIcon = iconRegister.registerIcon(Constants.modId + ":" + getTileName());
		topIcon = iconRegister.registerIcon(Constants.modId + ":" + getTileName() + "_top");
		bottomIconDisabled = iconRegister.registerIcon(Constants.modId + ":" + getTileName() + "_bottom_0");
		bottomIconEnabled = iconRegister.registerIcon(Constants.modId + ":" + getTileName() + "_bottom_1");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int side, int meta) {
		return ((side == 0)
				? (BetterStorage.globalConfig.getBoolean(GlobalConfig.enableStationAutoCrafting)
						? bottomIconEnabled : bottomIconDisabled)
				: ((side == 1) ? topIcon : blockIcon));
	}
	
	@Override
	public TileEntity createNewTileEntity(World world) {
		return new TileEntityCraftingStation();
	}
	
}
