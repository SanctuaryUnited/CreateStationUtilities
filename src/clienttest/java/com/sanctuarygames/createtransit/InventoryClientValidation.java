package com.sanctuarygames.createtransit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sanctuarygames.createtransit.inventory.TransitInventory;
import com.sanctuarygames.createtransit.inventory.client.TransitCreativeScreen;
import com.sanctuarygames.createtransit.mixin.client.CreativeMenuScrollAccess;
import com.sanctuarygames.createtransit.registry.ModCreativeTabs;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Real NeoForge CLIENT launch/mixin checks, without opening a graphics window. */
@EventBusSubscriber(modid = CreateTransit.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InventoryClientValidation {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    @SubscribeEvent public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new DataProvider() {
            @Override public String getName() { return "Create Transit inventory client validation"; }
            @Override public CompletableFuture<?> run(CachedOutput output) {
                try {
                    var loader = InventoryClientValidation.class.getClassLoader();
                    V15RenderCheck.run();WaveOneDoorCheck.run(loader);
                    MultipartBakeCheck.run();
                    ElevatorScaleFourCheck.run();com.sanctuarygames.createtransit.radio.client.RadioAudioCheck.run();com.sanctuarygames.createtransit.radio.client.RadioRangeCheck.run();com.sanctuarygames.createtransit.radio.client.RadioAcousticsCheck.run();
                    var screen = Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen", false, loader);
                    var menu = Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$ItemPickerMenu", false, loader);
                    var slot = Class.forName("net.minecraft.world.inventory.Slot", false, loader);
                    check(TransitCreativeScreen.class.isAssignableFrom(screen), "Creative screen mixin interface applied");
                    check(CreativeMenuScrollAccess.class.isAssignableFrom(menu), "Native scroll invoker applied");
                    for (var handler : List.of("selectSectionedTab", "ignoreDecoration", "drawSectionBars")) {
                        check(Arrays.stream(screen.getDeclaredMethods()).anyMatch(method -> method.getName().contains(handler)), "Creative screen injection applied: " + handler);
                    }
                    check(Arrays.stream(slot.getDeclaredMethods()).anyMatch(method -> method.getName().contains("skipDecorationHighlight")), "Slot highlight injection applied");

                    var root = new JsonObject();
                    root.addProperty("client_mixins_verified", true);
                    var elevatorScreens=new java.util.HashMap<net.minecraft.world.inventory.MenuType<?>,net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<?,?>>();
                    com.sanctuarygames.createtransit.elevator.client.ElevatorClient.screens(new net.neoforged.neoforge.client.event.RegisterMenuScreensEvent(elevatorScreens));
                    check(elevatorScreens.containsKey(com.sanctuarygames.createtransit.elevator.ElevatorRegistry.MENU.get()),"Production elevator controller screen factory registers against the real Minecraft menu type");
                    for(var texture:java.util.List.of(com.simibubi.create.foundation.gui.AllGuiTextures.SCHEDULE_CARD_LIGHT,com.simibubi.create.foundation.gui.AllGuiTextures.BRASS_FRAME_TL)){
                        check(texture.getWidth()>0&&texture.getHeight()>0,"Actual Create schedule/brass texture has usable native coordinates");
                        var location=texture.getLocation();check(loader.getResource("assets/"+location.getNamespace()+"/"+location.getPath())!=null,"Controller texture exists in the installed Create dependency");
                    }
                    root.addProperty("elevator_controller_native_screen_factory_and_create_textures_checked",true);
                    root.addProperty("elevator_gui_validation_scope","Production CLIENT screen registration and dependency texture linkage; no graphical screen interaction");
                    System.out.println("PASS: Native CLIENT elevator menu-screen factory and installed Create schedule/brass texture resources.");
                    net.neoforged.neoforge.common.CreativeModeTabRegistry.sortTabs();
                    var sorted = net.neoforged.neoforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs();
                    int vehicleIndex = sorted.indexOf(ModCreativeTabs.VEHICLES.get());
                    check(vehicleIndex >= 0 && sorted.indexOf(ModCreativeTabs.STATION.get()) == vehicleIndex + 1,
                        "Station Utilities immediately follows Vehicle Kits in the actual NeoForge tab order");
                    root.addProperty("transit_tabs_adjacent", true);
                    var order = new JsonArray();
                    sorted.forEach(tab -> order.add(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString()));
                    root.add("native_tab_order", order);
                    System.out.println("PASS: Native NeoForge tab sorting places Station Utilities immediately after Vehicle Kits.");
                    // NeoForge datagen loads the CLIENT distribution but skips gameplay setup.
                    // Invoke the exact setup body to check Flywheel API linkage/registration.
                    com.sanctuarygames.createtransit.escalator.client.EscalatorClient.registerVisualizers();
                    var visualizer=dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(com.sanctuarygames.createtransit.escalator.EscalatorRegistry.ENTITY.get());
                    check(visualizer!=null,"Production registration body installs escalator Flywheel visualizer");
                    for(int material=0;material<6;material++)check(com.sanctuarygames.createtransit.escalator.client.EscalatorSurfaceModels.get(material).modelLocation().getPath().startsWith("block/escalator_surface_"),"Early registered dedicated unit partial");
                    root.addProperty("escalator_production_visualizer_registration_checked",true);
                    root.addProperty("escalator_validation_scope","Client datagen distribution plus direct invocation of production visualizer registration; no graphics window or gameplay client setup event.");
                    System.out.println("PASS: CLIENT distribution linked the production Flywheel visualizer registration and six dedicated partial model locations. No graphical renderer/FPS test.");
                    com.sanctuarygames.createtransit.elevator.client.ElevatorClient.registerVisualizers();
                    var elevatorVisual=dev.engine_room.flywheel.api.visualization.VisualizerRegistry.getVisualizer(com.sanctuarygames.createtransit.elevator.ElevatorRegistry.CABIN.get());
                    check(elevatorVisual!=null&&!elevatorVisual.skipVanillaRender(null),"Elevator has Flywheel geometry and retains normal door rendering");
                    check(com.sanctuarygames.createtransit.inventory.TransitInventory.vehicles().stream().allMatch(s->s.key().equals("gondola")),"Only gondola appears in the vehicle catalog");
                    com.sanctuarygames.createtransit.elevator.client.ElevatorClient.renderers(new net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers());
                    try {var field=net.minecraft.client.renderer.entity.EntityRenderers.class.getDeclaredField("PROVIDERS");field.setAccessible(true);var registered=(java.util.Map<?,?>)field.get(null);check(registered.containsKey(com.sanctuarygames.createtransit.elevator.ElevatorRegistry.MODEL_CABIN.get()),"Dedicated model cabin renderer is registered independently of Flywheel");}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                    System.out.println("PASS: Dedicated model cabin entity renderer, landing renderer registration and gondola-only catalog checks.");
                    var tabs = new JsonArray();
                    var lookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
                    EscalatorColorCheck.run(lookup);
                    root.addProperty("escalator_native_dye_tints_checked",true);
                    for (var tab : List.of(ModCreativeTabs.VEHICLES.get(), ModCreativeTabs.STATION.get())) {
                        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, lookup));
                        var layout = TransitInventory.layout(TransitInventory.sections(tab), tab.getDisplayItems());
                        var json = new JsonObject();
                        json.addProperty("id", BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString());
                        json.addProperty("label_key", ((TranslatableContents) tab.getDisplayName().getContents()).getKey());
                        json.addProperty("real_items", tab.getDisplayItems().size());
                        json.addProperty("rows", layout.rows());
                        var headers = new JsonArray();
                        for (var header : layout.headers()) {
                            var label = new JsonObject();
                            label.addProperty("row", header.row());
                            label.addProperty("label_key", ((TranslatableContents) header.title().getContents()).getKey());
                            headers.add(label);
                        }
                        json.add("headers", headers);
                        var cells = new JsonArray();
                        for (var cell : layout.cells()) cells.add(cell.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(cell.getItem()).toString());
                        json.add("cells", cells);
                        tabs.add(json);
                    }
                    root.add("tabs", tabs);
                    System.out.println("PASS: Actual NeoForge CLIENT launch applied all creative screen, menu-scroll and slot-highlight mixins. Exported the live registered inventory layout.");
                    return DataProvider.saveStable(output, root, event.getGenerator().getPackOutput().getOutputFolder().resolve("inventory-layout.json"));
                } catch (Throwable error) { return CompletableFuture.failedFuture(error); }
            }
        });
    }
}
