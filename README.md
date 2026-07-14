# Data Machines

Data Machines is a powerful, data-driven plugin that allows server owners to create fully functional custom machines without writing a single line of Java code. By translating configuration files into Minecraft functionality, it enables the creation of a vast array of industrial and utility machines.

## Features

- **Zero-Code Configuration**: Define your machines and recipes entirely through YAML configuration files.
- **Flexible Recipe System**: Support for multiple recipe types (e.g., smelting, crushing, mixing, sorting) and probabilistic outputs.
- **Customizable Machine Behavior**: Control how machines interact with inventories, redstone, permissions, and fuel.
- **Nexo Integration**: Seamlessly integrates with Nexo to provide custom block and item representations for your machines.

## Configuration

The plugin uses two primary configuration files: `recipes.yml` and `machines.yml`.

### Recipes (`recipes.yml`)

Recipes define what a machine can process and what it produces.

#### Recipe Structure

```yaml
recipes:
  recipe_id:
    type: "recipe_type" # The type of recipe (e.g., "smelting", "crushing")
    inputs:
      - { item: "item_id", amount: 1 }
    outputs:
      - { item: "item_id", amount: 1, chance: 1.0 }
    processing_time: 200 # Processing time in ticks (20 ticks = 1 second)
```

#### Example

```yaml
recipes:
  iron_ingot_from_ore:
    type: "smelting"
    inputs:
      - { item: "iron_ore", amount: 1 }
    outputs:
      - { item: "iron_ingot", amount: 1, chance: 1.0 }
    processing_time: 200
```

### Machines (`machines.yml`)

Machines define the physical and environmental behavior of the machine.

#### Machine Structure

```yaml
machines:
  machine_id:
    machine_name: "Machine Name" # The title of the machine's inventory
    supported_recipe_types: ["type1", "type2"] # List of recipe types this machine can process
    input_slots: [0] # Slots in the machine inventory that are used for inputs
    output_slots: [1, 2] # Slots in the machine inventory that are used for outputs
    fuel_slots: [3] # (Optional) Slots used for fuel
    modifiers: 1.0 # (Optional) Speed multiplier. Can be a single value or a map of recipe_type -> multiplier.
    properties:
      always_drop: false # If true, items are dropped at the machine location
      stop_on_full_buffer: true # If true, machine stops if internal buffer is full
      allow_pushing: true # If true, machine tries to push items to adjacent inventories
      redstone_required: "disabled" # Redstone state: "on", "off", "disabled"
      fuel_required: false # (Optional) Whether the machine consumes fuel
      fuel_items: ["coal", "charcoal"] # (Optional) List of items that can be used as fuel
      requires_permission: "permission.node" # (Optional) Permission required to use the machine
    progress_elements:
      10: texture1 # (Optional) Map of progress percentage -> texture to change block texture during processing
```

#### Example

```yaml
# Example of a complex machine
  auto_sorter:
    machine_name: "Auto Sorter"
    supported_recipe_types: ["sorting"]
    input_slots: [0]
    output_slots: [1, 2]
    modifiers: 1.0
    properties:
      stop_on_full_buffer: true
      allow_pushing: true
      redstone_required: "on"
      fuel_required: false
      requires_permission: "datamachines.sorting"
```

## Output Precedence

When a machine finishes processing a recipe, it follows these rules to determine where the produced items go:

1. **Always Drop**: If `always_drop` is true, the items are dropped at the machine's location.
2. **External Push**: If `allow_pushing` is true, the plugin tries to deposit items into an adjacent inventory.
3. **Internal Buffer**: If neither of the above are true (or `allow_pushing` is false), items are placed in the machine's own output slots.
4. **Overflow**: If the internal buffer is full and `stop_on_full_buffer` is false, the items are dropped at the machine's location.

## Requirements

- **Nexo**: Required for custom block and item representations.

## TODO

- Implement updating gui textures based on machine progress.
- Implement updating gui textures based on fuel level.

---

## AI Usage Disclaimer

Generative AI is used in the development of this plugin. Only local models (on my PC) is used, no cloud models (AI Datacenters) will ever be used.

The project is classified as level 3 on the self-reported [REAL rating](https://www.realgoodai.org/real-rating)

<img src="img/3REALrating.png" alt="drawing" width="200"/>