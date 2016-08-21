# Flowers

This program generates meshes of 3D geometrical forms and exports them in a `.ply` file.
It uses the [Morphogen library](https://github.com/thi-ng/morphogen/) by Karsten Schmidt.

![example](spritz.png)

## Usage

Clone this repository and `cd` in the folder. Launch the REPL `lein repl`
To export a mesh, like that one created in the `spritz` function, use `(apply save-mesh (spritz))`
A new file with `.ply` extension will be created in the main project folder.

To render them with Blender, follow the instruction provided in t


