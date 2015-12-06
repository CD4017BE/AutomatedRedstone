package cd4017be.circuits.block;

import net.minecraft.block.material.Material;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.templates.BlockPipe;

public class BlockInvConnector extends BlockPipe 
{

	public BlockInvConnector(String id, Material m) 
	{
		super(id, m, DefaultItemBlock.class, 0x20, "InvConB", "InvConC", "InvConL");
		this.size = 0.375F;
	}

}
