# Agentic Development Guide: Data Machines

This document serves as the primary source of truth and the operational manual for AI agents working on the **Data Machines** project. All agents must adhere to the following guidelines to ensure architectural integrity and the "North Star" of the project.

## Primary goal
**"Zero-Code Custom Machines"**
The primary goal is to allow server owners to implement fully functional custom machines without writing a single line of Java code. This is achieved by translating a data-driven configuration into Minecraft functionality.

### The Law of the Handler
- **Configuration is the Source of Truth**: The behavior of every machine is defined in configuration files.
- **Predefined Logic Handlers**: All machine logic is encapsulated within `Handler` classes. 
- **Extensibility**: Adding a new *category* of machine (e.g., a "Quantum Teleporter") may require the implementation of a new `Handler` class. Adding a new *instance* of a machine (e.g., a "Iron Macerator" vs. a "Gold Macerator") is done purely via configuration. Some instances of machines may overlap on handlers, for example both a furnace and a macerator may share the `BasicMachineHandler` if no additional logic is required between their functionality.

## Technical Stack
- **Language**: Java 25 (Use modern features: Sealed classes, Pattern matching, Records).
- **Platform**: Paper API 26.1.2.
- **Build Tool**: Gradle.
- **Integration**: Nexo (utilizing the "Furniture Mechanic" approach for block/item representation).

## Core Concepts & Atomic Operations

### 1. The Data-Functionality Gap
The plugin translates configuration files into active Minecraft logic. Agents must treat the data format as a first-class citizen.

#### Data Model Specification
- **Recipes (`recipes.yml`)**: Defined by a `type` which maps directly to a `Handler` implementation. Contains `inputs` (list of items/amounts), `outputs` (list of items/amounts/chance), and `processing_time` (ticks).
- **Machines (`machines.yml`)**: Defines the physical and environmental behavior.
    - `machine_name`: The title of the machine's inventory. Defaults to the machine ID.
    - `supported_recipe_types`: A list of recipe types this machine can process. **Required**
    - `input_slots`: A list of slots in the machine inventory that should be handled as input slots. **Required**
    - `output_slots`: A list of slots in the machine inventory that should be handled as output slots. **Required**
    - `fuel_slots`: A list of slots in the machine inventory that should be handled as fuel slots (if fuel is required, otherwise these will be disabled).
    - `modifiers`: Can be a single numeric value (global speed multiplier) or a map of `recipe_type -> multiplier`.
    - `progress_slot`: The slot index in the machine's inventory that holds the item used to display progress. This item is renamed dynamically based on the current processing progress.
    - `progress_textures`: A map of `progress -> texture` (e.g., `0: "Idle", 50: "Halfway", 100: "Done"`) used to rename the item in the `progress_slot`. If omitted, no renaming occurs.
    - `fuel_progress_slot`: The slot index in the machine's inventory that holds the item used to display remaining fuel. This item is renamed dynamically based on the remaining fuel timer.
    - `fuel_progress_elements`: A map of `remaining -> texture` (e.g., `0: "No fuel", 50: "Half fuel", 100: "Full"`) used to rename the item in the `fuel_progress_slot`. If omitted, no renaming occurs.
    - `properties`:
        - `always_drop`: Flag to chose if the machine will always drop (i.e. not use an internal buffer and not push to adjacent inventories). Default: false.
        - `stop_on_full_buffer`: Flag to chose if the machine will stop working if the internal buffer is full (internal buffer is one stack for each output item, if either stack is full the buffer is considered full). Default: true (not used if `always_drop` is true)
        - `allow_pushing`: Flag to chose if the machine will automatically push items from its internal buffer to adjacent inventories. Default: true (not used if `always_drop` is true)
        - `redstone_required`: Redstone state requirement (`on`, `off`, `disabled`). Default: `disabled`.
        - `fuel_required`: Boolean indicating if the machine consumes fuel. Default: false.
        - `fuel_items`: A list of item IDs that are allowed in fuel slots. If empty/null, any item is accepted.
        - `requires_permission`: A string value indicating the permission required to use the machine, if none is set, no permission check will be performed.
    -  Machines should follow the following precedence rules for outputting processed items:
      -  If `always_drop` property is true, drop the produced item at the machine location.
      -  If `allow_pushing` property is true, try deposit to external inventory, else produce to internal buffer.
      -  If internal buffer is full and `stop_on_full_buffer` property is false, newly produced items should be dropped at the machine location.

### 2. Machine Lifecycle
Every machine follows a basic lifecycle:
`Input(s) -> Processing -> Output(s)`

### 3. Atomic Processing Operations
To prevent architectural drift, all machine logic must be implemented using these core processing types:
- **Temporal Processing**: Logic that takes a specific amount of time (ticks).
- **Conditional Processing**: Logic that required a specific condition to be met (e.g., Redstone signal, Fuel, specific nearby blocks).
- **Probabilistic Processing**: Logic that has a chance to fail or produce bonus outputs.

### 4. Configurable Output Behaviors
Output methods are data-configurable. A machine may be:
- **Internal Buffer**: Items are placed in the machine's own inventory.
- **External Push**: Items are pushed into an adjacent inventory.
- **Loot Drop**: Items are dropped at the machine's location.

## Agentic Workflow

### 1. Data-First Approach
Whenever a new feature or machine is requested:
1. **Propose the Data Format**: Propose the a configuration snippet for the same.
2. **Propose the Implementation**: Propose the Java code to handle that data.
3. **Verify**: Ensure the data and code are in sync.

### 2. Implementation Strategy
- **Modular Logic Handlers**: Do not use giant switch statements in the main plugin class. Create a dedicated `Handler` for each logic type.
- **Nexo Integration**: Research the Nexo "Furniture Mechanic" to ensure blocks are placed and interact with the laway the intended.
- **Concurrency**: Minimize main thread impact. Use async tasks for data loading and non-blocking operations.

### 3. Verification & Documentation
- **Baseline Test Cases**: Use the sample configurations in `recipes.yml` and `machines.yml` as the primary test suite to verify that any new `Handler` or core logic changes do not break existing machine behaviors.
- **Integration Testing**: Use `./gradlew runServer` to launch a Minecraft server and verify the same in-game.
- **Documentation**: If a architectural decision is made during implementation, update this `AGENTS.md` file immediately.
- **Build**: Always run build after implementing a feature to ensure the codebase is in a good state before passing it along.

## Common Pitfalls
- **Breaking the Zero-Code Promise**: Adding a feature that requires a new `Handler` for every single machine instance.
- **Naive Processing**: Implementing a "processing" method that ignores the Temporal or Conditional requirements.
- **Casting Errors**: Avoid unsafe casts when dealing with Nexo API.