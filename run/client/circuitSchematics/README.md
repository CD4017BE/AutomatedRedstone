#Exporting and importing schematics

When shift-clicking the schematic save button in the Circuit Designer then it won't store the schematic on the item in the slot but instead in a `.dat` file named after your schematic that is located in the `circuitSchematics`-subfolder of your Minecraft instance. You can then access that schematic again from any Circuit Designer in any save game (even on multiplayer servers you join) by entering its name into the text field and then shift-clicking the schematic load button.

#Examples and tutorials

This directory contains some example schematics for various useful circuits. These are intended to help people learn the mechanics.
To get ingame access to these schematics, just download the files, put them into your circuitSchematics folder and follow the above steps (no game restart required).

###List of circuits:
- `SimpleClock`: Has a single output that emits a square wave signal that switches between ON and OFF every cycle.

###Test Circuits
It is highly recommended that you try to build these circuits your self as kind of puzzle exercise before just using the provided solutions.

Therefore most of these circuits come with an associated test circuit that is meant to test whether your circuit behaves correctly. It does that by feeding it certain inputs and analyzing the resulting output. In addition to the inputs and outputs used to communicate with the circuit to be tested, the Test circuit has another validation output that is ON when the circuit behaves correctly and OFF when not. So your circuit has passed the test when that validation output stays ON all the time.

To connect both circuits together, either place them directly next to each other and let them transmit across the connecting face (using different bit channels if there is more than 1 input or more than 1 output). Or use one or multiple cables in between, eventually also hooked up to displays/oscilloscopes to see what's going on.
