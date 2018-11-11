# Exporting and importing schematics

When shift-clicking the schematic save button in the Circuit Designer then it won't store the schematic on the item in the slot but instead in a `.dat` file named after your schematic that is located in the `circuitSchematics`-subfolder of your Minecraft instance. You can then access that schematic again from any Circuit Designer in any save game (even on multiplayer servers you join) by entering its name into the text field and then shift-clicking the schematic load button.

# Examples and tutorials

This directory contains some example schematics for various useful circuits. These are intended to help people learn the mechanics.
To get ingame access to these schematics, just download the files, put them into your circuitSchematics folder and follow the above steps (no game restart required).

### List of circuits:
- `SimpleClock`: Has a single output that emits a square wave signal that switches between ON and OFF every cycle.
- `RS_Latch`: Has two inputs and one output that turns ON when receiving an ON signal on the one input and turns OFF(0) when receiving an ON signal on the other input. While no input is ON the output state remains constant. When both inputs are ON at the same time the output may be either On or OFF depending on implementation.
- `3tDelayline`: Has one input and one output that always emits the value the input received 3 cycles ago. (easily expendable to any N-cycle delay line)
- `R-EdgeDetector`: Emits a one cycle long ON pulse though its output on every rising edge of the input signal (transition from OFF to ON), otherwise the output is OFF. (A falling edge detector would be very similar)
- `PulseCounter`: Emits a one cycle long ON pulse though its output after every 5-th rising edge in its input signal, otherwise the output is OFF. (counts incoming pulses and resets at 5)
- `MemoryCell`: Has a (8-bit numeric) state input and a control input. When control input is ON, the output changes to the value of the state input otherwise it stays the same (ignoring the state input).

### Test Circuits
It is highly recommended that you try to build these circuits your self as kind of puzzle exercise before just using the provided solutions.

Therefore most of these circuits come with an associated test circuit that is meant to test whether your circuit behaves correctly. It does that by feeding it certain inputs and analyzing the resulting output. In addition to the inputs and outputs used to communicate with the circuit to be tested, the Test circuit has another validation output that is ON when the circuit behaves correctly and OFF when not. So your circuit has passed the test when that validation output stays ON all the time.

To connect both circuits together, either place them directly next to each other and let them transmit across the connecting face (using different bit channels if there is more than 1 input or more than 1 output). Or use one or multiple cables in between, eventually also hooked up to displays/oscilloscopes to see what's going on.
