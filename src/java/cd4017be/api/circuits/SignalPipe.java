package cd4017be.api.circuits;

import java.util.Collection;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Serves as signal provider for one Chip's input pin and as signal receiver for another (or multiple) Chip's output pin.
 * @author cd4017be
 */
public class SignalPipe implements IntConsumer, IntSupplier {

	/** signal state */
	public int state;
	/** list of signal receiver notifiers */
	public IntConsumer[] receivers;

	/**
	 * @param initState the initial state of this pipe
	 * @param receivers list of signal receiver notifiers that get called when this pipe's state changes
	 */
	public SignalPipe(int initState, IntConsumer... receivers) {
		this.state = initState;
		this.receivers = receivers;
	}

	/**
	 * @param receivers new list of signal receiver notifiers
	 */
	public void setReceivers(Collection<IntConsumer> receivers) {
		this.receivers = receivers.toArray(new IntConsumer[receivers.size()]);
	}

	/**
	 * @return this pipes current state
	 */
	@Override
	public int getAsInt() {
		return state;
	}

	/**
	 * sets the state of this pipe and notifies all signal receivers if it changed
	 * @param value the new state
	 */
	@Override
	public void accept(int value) {
		if (value != state) {
			state = value;
			for (IntConsumer r : receivers)
				r.accept(value);
		}
	}

}
