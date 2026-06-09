# Solenoid — Project Context & State Tracking

## 🟢 Implemented & Confirmed (Build Passed)

* **Status:** Clean compilation, 175 valid JSONs generated, `runData` passes cleanly, Curios loads correctly.
* **Magnet Charm (`magnet_charm`):**
  * Curios item made of neodymium.
  * Vacuums nearby drops while worn (`curioTick`) or held in the inventory.
  * Drains EMF; features a right-click toggle function.
  * Curios dependency added: version `15.0.0-beta.2` (range `[15.0.0-beta.1,)`).
* **Repulsor (`repulsor`):**
  * Rechargeable neodymium gadget (Capacity: 30k EMF).
  * Wearable or usable from hand.
  * Right-click pulse: flings mobs outward and upward, deflects projectiles.
  * Cost: 500 EMF/pulse. Cooldown: 40 ticks. Fizzles out if energy is under 500 EMF. Charges inside the Capacitor block.
* **Monazite Processing Line:**
  * **7 Items:** Sawdust, lye, crushed monazite, monazite concentrate, rare-earth cake, thorium sludge, phosphate.
  * **3 Machines:**
    * *Chemical Reactor:* 2-slot processing.
    * *Digester:* 5-slot processing requiring a reagent.
    * *Centrifuge:* 4-slot processing.
    * *Specs:* Each includes block + BlockEntity (BE) + custom menu + code-drawn screen. EMF data split across two data-slots. Fully wrenchable; retains loot tables.
  * **Recipes & Integration:** 8 custom recipes across 3 recipe types; 3 distinct JEI categories implemented.

---

## 🟡 Open Flags & Tech Debt from Last Build

* **Item IDs:** Using correct names explicit to the item (`magnet_charm`, `repulsor`), not the generic `item_*` prefix.
* **Repulsor Recipe:** Crafted via `4 magnet + 4 coil + 1 screen`.
* **Shortcut Cleanup:** The legacy `monazite_separating.json` recipe shortcut (Raw → Dusts) was left in intentionally. *Action item: Remove this if the multi-machine chain should be the exclusive path.*
* **Placeholders:**
  * Machine block textures currently copy/paste and reuse the Crusher's textures.
  * New item textures are utilizing the temporary dust placeholders.
* **Verification:** `runClient` has not yet been executed to visually confirm GUI/screen rendering behavior.

---

## 🔬 Monazite Chain Architecture & Design

### Process Flow (Stops at Centrifuge)

1. **Raw Monazite** → *Crusher* → **Crushed Monazite** → *Separator (Magnetic)* → **Monazite Concentrate** (+ Sand)
2. **Monazite Concentrate** + **Lye** → *Digester* → **Rare-Earth Cake** + **Thorium Sludge** + **Phosphate**
3. **Phosphate** → *Byproduct* → Craftable into Bone Meal.
4. **Rare-Earth Cake** → *Centrifuge* → **Cerium Dust ×2** + **Neodymium Dust ×1** → *Induction Furnace* → **Ingots**.
5. **Thorium Sludge** → *Centrifuge* → **Thorium Dust** → *Induction Furnace* → **Ingots**.

### Reagent Production (Real-world Wood-Ash Route)

* **Planks** → *Crusher* → **Sawdust** → *Chemical Reactor* → **Lye** (NaOH/caustic base).

### Element Tint Standards

| Element | Hex Code | Visual Profile |
| :--- | :--- | :--- |
| **Cerium** | `#D9C76A` | Pale gold |
| **Neodymium** | `#B57BD6` | Lilac |
| **Thorium** | `#4A5246` | Dark grey-green |

---

## 🎨 Asset Pipeline & Textures

### Deterministic Deliverables (PNGs Saved)

* `dust_template.png`: Neutral 16px greyscale tintable dust (5-level gradient created by desaturating the original red dust asset).
* `cerium_dust.png` / `neodymium_dust.png` / `thorium_dust.png`: 16px outputs calculated by `template × tint`.
* `cerium_ingot.png` / `neodymium_ingot.png` / `thorium_ingot.png`: 32px outputs calculated via `ingot_grey_full.png template × tint`.
* `thorium_pellet.png`: 16px recolored to Thorium dark grey-green with a distinct green emission/glow map on top.

### Nano Banana Generation Prompts (Pending Final Rescale & Export)

* `raw_monazite`: Chunk asset recolored to amber, 32px.
* `cerium_copper_battery`: Copper body, cerium-gold accent band, steel cap.
* `rare_earth_cake`: Industrial filter-cake appearance (prompt specifically flattens + desaturates to prevent it looking like edible food).
* `lye`: Off-white powder framed on a strict magenta (`#FF00FF`) background so alpha transparency cleanup scripts don't eat the white dust edges.
* **Machine Facings:** 3 distinct fronts (Reactor: green viewport / Digester: heated tank with orange vents / Centrifuge: spinning rotor lines) + shared `machine_side`.
* **Structural Blocks:** Machine frame (recessed dark interior, solid/not hollow) + assembly screw (sharp diagonal threading).
* **Power Blocks:** RTG front (green core indicator + hazard striping) + cooling fin side / Recharger front (power socket + charge bolt icon).
* *Note:* Items are scaled to 16px, ores/ingots to 32px, and block textures handled via `--block` script flags.

---

## 🛠️ CLI Agent Prompts & Development Roadmap

### Completed Prompts ✅

* `Magnet Charm Base Implementation`
* `Repulsor Item Logic & Mechanics`
* `Monazite Line Core System (Items, Recipes, Machines, Menus)`

### Written but Unconfirmed Blocks (Next Up for Build Testing)

1. **Machine Texture Wiring:** Update assets-only configuration. Replace crusher placeholders with orientable models, facing blockstates, and the newly generated front/side textures.
2. **RTG Block:** Passive generator (~8 EMF/t), no UI or fuel slot, pushes power directly to adjacent cables/blocks. Registers the `thorium_pellet` item. Recipe: `I I I / C P C / I I I` (I=Iron Ingot, C=Coil, P=Pellet).
3. **Recharger Block:** Draws EMF from connected grids, charges any item storing energy placed in its internal slot at a rate of 200 EMF/t. Features a custom code-drawn GUI.

### Lore & Future Gadgets

* **Thorium Mantle Lamp:** Static, permanently lit block (Light Level 15). Uses a torch baked directly into the crafting recipe as its ignition source.
  * *Chain:* Thorium Dust + Small Cerium Dust (New item: 4 small = 1 dust, acts as the real-world 1% candoluminescence activator) → Thorium Mantle. Mantle + Torch + Glass → Lamp.
* **Gadget Shortlist:** Magnet Gun, Coilgun, Magnetic Jump Pad, Mob Magnet, Disarm Field, Incendiary Grenade, Glow Paint, Tracer Slugs, Force-Field Generator.

---

## 📐 Workflow & Safety Constraints

* **Texture Automation:** `Nano Banana Prompt` → `rescaleScript (--item/--block --size --colors)` → `src/main/resources/assets`. Always include anti-noise wording in prompts; near-white assets use a solid magenta canvas background.
* **Agent Logic Guarantees:** Structure requirements using strict `CONTEXT / GOAL / DELIVERABLES / CONSTRAINTS / ACCEPTANCE` frameworks.
* **Code Guardrail:** "Verify all method signatures directly against decompiled NeoForge 26.1.x sources—no guessing or hallucinating deprecated mappings." Absolutely zero Coremods/Mixins allowed unless explicitly requested. Define item properties cleanly on the `Properties` block instantiation. Screens must be code-drawn UI elements.

---

## 📣 Marketing & Promotion Strategy

* **Current Metrics:** 37 CurseForge downloads (assumed web crawlers/bots; do not treat as an active player base yet).
* **Launch-Ready Bar:** Seamless signature gameplay loop, zero placeholder textures, zero edge-case crashes, full JEI integration, and high-quality gameplay media (GIFs of the Magnet Gun, Mob-Magnet mob farms, and the moving Magnetic Separator).
* **Distribution Strategy:**
  1. Cross-post immediately to Modrinth.
  2. Polish description pages with unified Markdown styling on both store platforms.
  3. Push a targeted post featuring a high-frame-rate GIF to `r/feedthebeast`.
  4. Announce on targeted developer/modding Discords.
  5. Pitch directly for modpack inclusions for exponential ecosystem growth
