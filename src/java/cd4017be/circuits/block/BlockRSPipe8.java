/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.block;

import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.templates.BlockPipe;
import net.minecraft.block.material.Material;

/**
 *
 * @author CD4017BE
 */
public class BlockRSPipe8 extends BlockPipe
{
    public BlockRSPipe8(String id, Material m)
    {
        super(id, m, DefaultItemBlock.class, 0x20);
        this.size = 0.5F;
    }
    
}
