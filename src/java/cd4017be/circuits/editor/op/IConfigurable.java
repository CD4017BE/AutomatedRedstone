package cd4017be.circuits.editor.op;


/**
 * @author CD4017BE
 *
 */
public interface IConfigurable {

	public String getCfg();

	public void setCfg(String cfg);

	/** @return text field tooltip (translation key) */
	public String cfgTooltip();

}
