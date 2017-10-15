/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.item;

import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.item.BaseItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class ItemRSPipe extends BaseItemBlock
{

	public ItemRSPipe(Block id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	protected void init() {
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 0), "rsp1bitN");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 1), "rsp1bitI");
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, 2), "rsp1bitO");
	}

	@Override
	public int getMetadata(int dmg) {
		return dmg;
	}

}
