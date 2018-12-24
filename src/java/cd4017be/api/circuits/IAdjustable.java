package cd4017be.api.circuits;


/**
 * Interface for {@link Chip} implementations that intend to give a player access to their internal state (via GUI).
 * @author CD4017BE
 */
public interface IAdjustable {

	/**
	 * @return array of parameter names
	 */
	String[] getLabels();

	/**
	 * @return array containing the current state of each parameter
	 */
	int[] getStates();

	/**
	 * attempts to change the value of a parameter (may have constraints)
	 * @param i parameter index
	 * @param v new value
	 */
	void setParam(int i, int v);

}
