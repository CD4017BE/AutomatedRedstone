package cd4017be.circuits.tis3d;

import li.cil.tis3d.api.SerialAPI;
import net.minecraftforge.fml.common.Optional;

public class API {

	@Optional.Method(modid = "tis3d")
	public static void register() {
		SerialAPI.addProvider(new SerialProvider());
	}

}
