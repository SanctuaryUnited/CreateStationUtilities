# Create: Station Utilities — V15 hallway and rendering update

Minecraft 1.21.1, NeoForge 21.1.248, Create 6.0.10, Java 21; internal version `0.2.0-dev15`, unchanged mod ID `createtransit`.

## Hallway indicators and call panels

- Six selectable indicator styles: Pixel Matrix, Slimline, Classic LED, Recessed Glass, Transit Sign and Brushed Metal. The header and side/call style selectors are independent. Previous four enum names are unchanged; existing configurations use their previous header style for the new side setting.
- Door indicator surfaces now sit outside their opaque backing on both finished sides. Arrows use actual pixel geometry rather than font glyphs. The mirrored header swaps positions while preserving each arrow's meaning.
- Each landing frame has stationary floor-ID plaques below its paired side indicators. Plaques use the linked landing ID (G, F1, B2, etc.). The header follows the nearest actual elevator floor marker during travel, independently of the landing plaque. Unlinked landings show `--`.
- Side arrows retain lime Up, red Down and gray inactive states, including existing arrival pulses.
- The call panel now draws upright arrow geometry directly. The previous general box helper reversed vertical UV coordinates, flipping the old Up/Down artwork despite correct server floor ordering.
- Hover highlights only the aimed arrow: lime Up or red Down. Other arrows retain their requested or inactive appearance. A pressed effect applies only to the clicked half and transitions to existing request indication. Disabled/fault panels ignore hover.
- The lowest reachable enabled floor offers Up only; the highest offers Down only; intermediate floors show both. Explicit marker links and arbitrary mounting height remain supported. Side/call style follows the owning controller, and frame accent dye remains supported.
- Existing FIFO requests remain floor requests; this does not add multi-car directional dispatch. Indicators remain integrated into landing/cabin door frames rather than separate registered blocks.

## Rendering work reduced

- Bounded immutable geometry cache for cabin bodies and stationary door poses.
- Landing frames and moving leaves render separately: an animation no longer rebuilds and retiles the whole frame.
- Cached client landing configuration, invalidated by block-entity updates, and bounded material-sprite lookups invalidated when the atlas sprite generation changes.
- Rectangular clipped texture tiles submit four vertices instead of two degenerate triangle-quads (eight vertices). Curved clipped polygons retain the triangle-fan path when needed. UVs, normals and animations remain exact; no motion quantization was introduced.
- Surface normals are calculated during tile construction, and opaque/glass passes share one tile lookup.
- V14.1's scoped multipart loading cache and moving-radio position correction remain included.

## Controls and upgrade

Install the new JAR on clients and server, replacing the previous Station Utilities/Transit JAR. Existing V14.1 cabins, floor links and offsets remain saved; no shaft rebuild is required for this patch.

Open the controller's Doors customization page. `Indicator style` chooses the header; `Side / call style` chooses the paired direction modules, plaques and linked call-button design. Scroll to reach both selectors at GUI scale 4. Floor IDs are still edited through Floors, independently of height.

## Validation

- Build/check completed successfully.
- All 187 required dedicated-server GameTests passed, including enabled terminal layouts, side-style propagation and panel state persistence.
- Native CLIENT checks passed 36 independent header/side-style NBT combinations and exact original-versus-split landing geometry at five animation positions.
- The 9,216-case native door geometry emitter submitted 25,184,256 vertices, versus 50,368,512 in V14.1; normalized UV, glass alpha and lighting checks passed. This is the mesh workload, excluding text and the new indicator artwork.
- A 5×5 curved-cabin body sample submitted 7,456 tiled vertices versus the previous fan path's 12,544 (40.6% fewer).
- A bounded 1,500-frame static body-generation/tile-lookup workload measured 157.26 ms for the previous call path and 63.10 ms using the geometry cache in this run. This measures CPU preparation, not total frame time.
- GUI scale 1–4 checks passed 1,088 native controls across 64 viewport cases. Existing audio, tint, inventory order and multipart loading-cache regressions passed.

The first full-resource screenshot pass caught reversed text facing; that issue was corrected before release. The final full-resource Minecraft client produced six style captures plus Up/Down hover captures. The header and both fixed F1 plaques were visually inspected and readable. Actual crosshair hits selected each intended arrow; only Up highlighted lime or Down red in the inspected images. The fixture also verified an Up-only lowest-floor panel and a raised call button explicitly linked to a middle-floor marker. See `V15_VISUAL_VALIDATION.log`. CPU/vertex measurements are renderer-workload evidence, not a promise of a particular GPU FPS increase. See `V15_VALIDATION.log`.

## Boundaries

Complementary Reimagined, the user's full modpack and two-client network latency still require pack testing. Native captures use software OpenGL without the user's shader pack. Shapes, colors and geometry are implemented in Java; the approved AI concept images are not used as texture atlases. Classic styling uses bold pixel lettering; this is not a manufacturer-exact numeric display reproduction.
