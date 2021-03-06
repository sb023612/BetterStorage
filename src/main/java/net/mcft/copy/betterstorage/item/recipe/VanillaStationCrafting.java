package net.mcft.copy.betterstorage.item.recipe;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.mcft.copy.betterstorage.BetterStorage;
import net.mcft.copy.betterstorage.api.crafting.ICraftingSource;
import net.mcft.copy.betterstorage.api.crafting.IRecipeInput;
import net.mcft.copy.betterstorage.api.crafting.RecipeInputBase;
import net.mcft.copy.betterstorage.api.crafting.StationCrafting;
import net.mcft.copy.betterstorage.config.GlobalConfig;
import net.mcft.copy.betterstorage.inventory.InventoryCraftingStation;
import net.mcft.copy.betterstorage.utils.StackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class VanillaStationCrafting extends StationCrafting {
	
	private final boolean onCreatedOverridden;
	
	public VanillaStationCrafting(World world, IRecipe recipe, ItemStack[] input, ItemStack output) {
		super(new ItemStack[]{ null, null, null, null, output }, createRecipeInput(world, recipe, input));
		// Only run isOnCreatedOverridden when auto-crafting is enabled.
		onCreatedOverridden = (GlobalConfig.enableStationAutoCraftingSetting.getValue()
				? isOnCreatedOverridden(output) : false);
	}
	
	private static IRecipeInput[] createRecipeInput(World world, IRecipe recipe, ItemStack[] input) {
		IRecipeInput[] requiredInput = new IRecipeInput[9];
		for (int i = 0; i < input.length; i++)
			if (input[i] != null)
				requiredInput[i] = new VanillaRecipeInput(world, recipe, input, i);
		return requiredInput;
	}
	
	@Override
	public boolean canCraft(ICraftingSource source) {
		return ((source.getPlayer() != null) || !onCreatedOverridden);
	}
	
	private static class VanillaRecipeInput extends RecipeInputBase {
		
		private final World world;
		private final IRecipe recipe;
		private final int slot;
		
		private final InventoryCrafting crafting;
		private final ItemStack expectedOutput;
		
		public VanillaRecipeInput(World world, IRecipe recipe, ItemStack[] input, int slot) {
			this.world = world;
			this.recipe = recipe;
			this.slot = slot;
			
			ItemStack[] craftingStacks = new ItemStack[input.length];
			for (int i = 0; i < input.length; i++)
				craftingStacks[i] = ItemStack.copyItemStack(input[i]);
			crafting = new InventoryCrafting(null, 3, 3);
			crafting.stackList = craftingStacks;
			
			expectedOutput = recipe.getCraftingResult(crafting).copy();
		}
		
		@Override
		public int getAmount() { return 1; }
		
		@Override
		public boolean matches(ItemStack stack) {
			ItemStack stackBefore = crafting.stackList[slot];
			crafting.stackList[slot] = stack;
			boolean matches = (recipe.matches(crafting, world) &&
			                   StackUtils.matches(expectedOutput, recipe.getCraftingResult(crafting)));
			crafting.stackList[slot] = stackBefore;
			return matches;
		}
		
	}
	
	// Utility functions
	
	private static Map<Class, Boolean> isOverriddenMap = new HashMap<Class, Boolean>();
	
	/** Checks if an item's {@link Item#onCreated onCreated} is overridden.
	 *  If this is the case, we need to call it before the item is pulled from
	 *  the output slot, so it allows for the ItemStack to be modified. For this
	 *  reason it is not possible for certain items to be crafted automatically. */
	private static boolean isOnCreatedOverridden(ItemStack output) {
		Class itemClass = output.getItem().getClass();
		Boolean overridden = isOverriddenMap.get(itemClass);
		// If there's already an entry in the map, return it.
		if (overridden != null) return overridden;
		
		for (String name : new String[]{ "onCreated", "func_77622_d" }) {
			try {
				Method onCreatedMethod = itemClass.getMethod(name, ItemStack.class, World.class, EntityPlayer.class);
				overridden = (onCreatedMethod.getDeclaringClass() != Item.class);
				break;
			} catch (NoSuchMethodException e) {
				// Ignore this exception, try the next method name.
			} catch (Throwable e) {
				BetterStorage.log.severe("Failed to check if onCreated is overridden for " + itemClass.getName() + ".");
				BetterStorage.log.severe("Please report this to the author who made the mod this item is from.");
				BetterStorage.log.severe("Tell them to contact copygirl if anything is unclear. Thanks!");
				e.printStackTrace();
				overridden = true;
				break;
			}
		}
		
		// This shouldn't happen, unless the method names change, for
		// example because of a Minecraft update, but check regardless.
		if (overridden == null)
			throw new Error("The item's onCreated method could not be found.");
		
		isOverriddenMap.put(itemClass, overridden);
		return overridden;
	}
	
	public static VanillaStationCrafting findVanillaRecipe(InventoryCraftingStation inv) {
		World world = ((inv.entity != null) ? inv.entity.worldObj : getClientWorld());
		InventoryCrafting crafting = new InventoryCrafting(null, 3, 3);
		crafting.stackList = inv.crafting;
		IRecipe recipe = findRecipe(crafting, world);
		if (recipe == null) return null;
		return new VanillaStationCrafting(world, recipe, inv.crafting, recipe.getCraftingResult(crafting));
	}
	
	@SideOnly(Side.CLIENT)
	private static World getClientWorld() { return Minecraft.getMinecraft().theWorld; }
	
	private static IRecipe findRecipe(InventoryCrafting crafting, World world) {
		for (IRecipe recipe : (List<IRecipe>)CraftingManager.getInstance().getRecipeList())
			if (recipe.matches(crafting, world))
				return recipe;
		return null;
	}
	
}
