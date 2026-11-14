# 物品/方块注册 API

通过 `net.aquiferic.diversitycore.api.registry.ContentRegistry` 在 Java 中链式声明物品、方块及其资源信息。声明提交后返回 NeoForge 延迟引用，实际游戏对象在注册事件中创建；模型、翻译、标签和掉落表由 datagen 生成。

## 在本项目中使用

在 `ModItems` 或 `ModBlocks` 中使用现有的 `ModRegistries.CONTENT`。`ModRegistries.register(bus)` 已在绑定注册表之前显式初始化这两个声明类，并挂载创造模式物品栏；添加新的声明类时，也要在 `CONTENT.register(bus)` 之前加载。

下面的物品示例可放入 `ModItems`：

```java
import static net.aquiferic.diversitycore.registry.ModRegistries.CONTENT;

import net.aquiferic.diversitycore.api.registry.ItemModelType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

public static final DeferredItem<Item> HAMMER = CONTENT.items().create("hammer")
    .maxDamage(256)
    .rarity(Rarity.UNCOMMON)
    .displayName("en_us", "Hammer")
    .displayName("zh_cn", "锤子")
    .model(ItemModelType.HANDHELD)
    .tag("c:tools/hammer")
    .register();
```

下面的方块示例可放入 `ModBlocks`：

```java
import static net.aquiferic.diversitycore.registry.ModRegistries.CONTENT;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

public static final DeferredBlock<Block> COPPER_CASING =
    CONTENT.blocks().create("copper_casing")
        .strength(3.0F, 6.0F)
        .soundType(SoundType.METAL)
        .properties(p -> p.mapColor(MapColor.METAL))
        .displayName("en_us", "Copper Casing")
        .displayName("zh_cn", "铜机壳")
        .tag("minecraft:mineable/pickaxe")
        .tagBoth("c:storage_blocks/copper")
        .register();
```

示例只声明普通物品和方块，不会自动赋予工具行为或机器逻辑。需自行提供对应 PNG。执行 `.\gradlew.bat runData` 将资源生成到 `src/generated/resources`。

## ID 与注册生命周期

`create(path)` 只接收当前 `ContentRegistry` 命名空间内的非空路径，例如 `hammer` 或 `tools/hammer`，不能传入 `diversitycore:hammer`。路径须符合 Minecraft `Identifier` 的格式。

- Builder 的 `register()` 提交声明并返回 `DeferredItem<T>` 或 `DeferredBlock<T>`。每个 Builder 只能提交一次，提交后不可修改。
- `ContentRegistry.register(bus)` 将物品和方块注册表绑定到模组事件总线，同时关闭声明；之后不能创建、修改或提交 Builder，也不能再次绑定。
- 声明阶段不要调用延迟引用的 `.get()`。在注册完成后的游戏逻辑或相应回调中获取实际对象。
- 同一注册表内不允许重复物品 ID 或重复方块 ID。方块默认同时占用同名物品 ID，不能再手动注册同名物品。作物自动生成的物品和方块也共享这些 ID 空间。

普通方块的 `register()` 返回方块引用，不返回生成的 `BlockItem`。注册完成后可通过 `block.get().asItem()` 获取物品；也可从 `blockDefinitions()` 中读取对应声明的 `item()`。

## 通用配置

物品和方块 Builder 都支持以下方法：

| 方法 | 行为 |
| --- | --- |
| `displayName(locale, name)` | 声明翻译；语言代码须匹配小写的 `xx_xx`，如 `en_us`、`zh_cn`；名称不能全为空白 |
| `texture(String)` / `texture(Identifier)` | 指定贴图资源 ID，不带 `textures/` 前缀和 `.png` 后缀 |
| `properties(configure)` | 按声明顺序累积执行原生属性配置回调 |

同一语言重复设置名称时，最后一次生效；`texture` 也以最后一次为准。贴图字符串使用原生 `Identifier.parse`，建议显式写命名空间，例如 `.texture("your_mod:item/hammer")`。默认物品贴图为 `<namespace>:item/<path>`，默认方块贴图为 `<namespace>:block/<path>`。

属性回调与工厂会在 NeoForge 创建对象时执行，不在链式声明时执行。应配置传入的属性对象，不要在回调中继续声明新内容。

## 物品配置

| 方法 | 说明 |
| --- | --- |
| `maxStackSize(size)` | 设置堆叠上限，范围为 `1` 至 `Item.ABSOLUTE_MAX_STACK_SIZE` |
| `maxDamage(durability)` | 设置正整数耐久值，具体物品行为遵循原生属性规则 |
| `rarity(Rarity)` | 设置稀有度 |
| `model(ItemModelType)` | `GENERATED` 为默认平面物品模型，`HANDHELD` 为手持模型 |
| `tag(String)` / `tag(TagKey<Item>)` | 将物品加入物品标签 |
| `properties(Consumer<Item.Properties>)` | 配置其他原生物品属性 |

堆叠和耐久等属性的组合及覆盖顺序遵循原生 `Item.Properties` 语义。

## 方块配置与物品形式

| 方法 | 说明 |
| --- | --- |
| `strength(value)` | 将硬度和爆炸抗性都设为 `value` |
| `strength(hardness, resistance)` | 分别设置硬度和爆炸抗性 |
| `soundType(SoundType)` | 设置方块声音 |
| `properties(Consumer<BlockBehaviour.Properties>)` | 配置其他原生方块属性 |
| `noItem()` | 禁止自动创建同名 `BlockItem` |
| `tag(String)` / `tag(TagKey<Block>)` | 添加方块标签 |
| `tagItem(String)` / `tagItem(TagKey<Item>)` | 添加生成的方块物品的物品标签 |
| `tagBoth(String)` | 分别向同名方块标签和物品标签添加对应成员 |

`tag` 不会自动复制到物品标签。`tagBoth` 创建的是两个不同注册表中的标签，不会将方块标签转换成物品标签。

```java
public static final DeferredBlock<Block> HIDDEN_MARKER =
    CONTENT.blocks().create("hidden_marker")
        .noItem()
        .tag("diversitycore:markers")
        .register();
```

`noItem()` 方块仍会生成模型、方块状态和已声明的翻译、方块标签，但生成空掉落表，也不会进入 `creativeItems()`。它不能配置 `tagItem` 或 `tagBoth`；先配置物品标签再调用 `noItem()` 也会报错。

自动生成的方块物品是普通 `BlockItem`，此 API 没有单独配置其属性或工厂的入口。如果需要自定义方块物品，可先用 `noItem()` 声明方块，再通过自己的物品注册和资源生成逻辑处理。

## 标签格式

标签字符串必须是完整的 `namespace:path`，不带 `#`。命名空间允许 `[a-z0-9_.-]`，路径允许 `[a-z0-9/._-]`；命名空间不能是 `.` 或 `..`，路径段不能是空字符串、`.` 或 `..`。例如 `c:tools/hammer` 有效，`tools/hammer` 和 `#c:tools/hammer` 无效。

`TagKey` 重载也会校验 ID 和目标注册表；物品标签与方块标签不能混用。可使用 `QS.itt("c:tools/hammer")` 或 `QS.blt("minecraft:mineable/pickaxe")` 创建经过校验的标签键。重复添加同一标签会去重，生成的标签文件采用追加语义，不替换其他数据包中的成员。

## 自定义物品和方块类型

`create(path, factory)` 接收使用原生属性对象的工厂，保留具体类型：

```java
// CustomItem extends Item，构造器接收 Item.Properties
public static final DeferredItem<CustomItem> CUSTOM_ITEM =
    CONTENT.items().create("custom_item", CustomItem::new)
        .maxStackSize(1)
        .register();

// 本项目已有的 CreativePowerBankBlock，构造器接收 BlockBehaviour.Properties
public static final DeferredBlock<CreativePowerBankBlock> POWER_BANK =
    CONTENT.blocks().create("example_power_bank", CreativePowerBankBlock::new)
        .strength(1.5F, 6.0F)
        .texture("diversitycore:block/creative_power_bank")
        .register();
```

工厂应使用传入的属性构造实例。自定义方块类的方块实体、能力、事件和渲染仍需自行注册；已有电能库的完整接入可参考 `ModBlocks` 与 `ModBlockEntities`。

## 自动生成的资源

本项目的 `ModDataGenerators` 已挂载所需 Provider：

| 内容 | 输出路径（相对于生成目录） |
| --- | --- |
| 独立物品模型 | `assets/<namespace>/models/item/<path>.json` |
| 物品模型定义（含方块物品） | `assets/<namespace>/items/<path>.json` |
| 方块模型 | `assets/<namespace>/models/block/<path>.json` |
| 方块状态 | `assets/<namespace>/blockstates/<path>.json` |
| 翻译 | `assets/<namespace>/lang/<locale>.json` |
| 物品/方块标签 | `data/<tag_namespace>/tags/item/<tag_path>.json`、`data/<tag_namespace>/tags/block/<tag_path>.json` |
| 方块掉落表 | `data/<namespace>/loot_table/blocks/<path>.json` |

普通方块使用单一贴图的 `cube_all` 模型及简单方块状态，方块物品引用该方块模型。默认掉落自身的方块物品，附带爆炸存活条件；`noItem()` 使用空掉落表。工厂返回自定义类也不会改变这些生成规则，需要朝向、多状态、特殊模型或特殊掉落时，应自行扩展或替换相应 Provider 的生成逻辑。配方需使用独立的[配方 API](recipes.md) 声明，作物请参阅[作物注册 API](crops.md)。

PNG 不由 API 生成，需放入 `src/main/resources/assets/<texture_namespace>/textures/item/` 或 `textures/block/` 等对应目录。项目会生成默认的 `en_us`、`zh_cn` 语言文件以及声明中使用的其他语言文件。

## 在其他模组中使用

创建自己的 `ContentRegistry`，在模组构造期间先提交全部声明，再绑定到该模组的事件总线：

```java
import net.aquiferic.diversitycore.api.registry.ContentRegistry;

public static final ContentRegistry CONTENT = new ContentRegistry("your_mod");

// 在模组构造器中，加载所有内容声明类之后：
CONTENT.register(bus); // bus 为该模组的 IEventBus
```

注册 API 仅保存资源声明，其他模组必须在自己的 datagen 监听器中挂载 Provider。在 `GatherDataEvent.Client` 中可使用：

```java
var output = event.getGenerator().getPackOutput();
event.addProvider(new ModModelProvider(output, CONTENT));
event.addProvider(new ModLanguageProvider(output, CONTENT, "en_us"));
event.addProvider(new ModLanguageProvider(output, CONTENT, "zh_cn"));
event.addProvider(new ModLootProvider(output, CONTENT, event.getReloadableLookupProvider()));
event.addProvider(new ModItemTagsProvider(output, CONTENT, event.getWorldLookupProvider()));
event.addProvider(new ModBlockTagsProvider(output, CONTENT, event.getWorldLookupProvider()));
```

这些 Provider 位于 `net.aquiferic.diversitycore.datagen`。模型 Provider 使用客户端类，监听器应只在客户端 datagen 环境加载；其他语言需另行添加 `ModLanguageProvider`。当前语言 Provider 还会写入固定值为 `Diversity Core` 的 `itemGroup.<namespace>` 翻译，其他模组使用时应按需调整。

`creativeItems()` 按内容声明顺序返回物品延迟引用，包含自动创建的方块物品。可在自己的创造模式物品栏 `displayItems` 回调中遍历并调用 `output.accept(item.get())`；API 不会自动替其他模组注册物品栏。`itemDefinitions()` 和 `blockDefinitions()` 则返回声明快照，供自定义资源生成使用。

## 验证

执行 `.\gradlew.bat verifyContentApi` 验证声明生命周期、延迟工厂、ID 冲突、标签校验以及生成的标签和掉落表；该任务已纳入 `check`。验证数据写入 `build/verification-data`，不打包到正式模组。模型和翻译等正式资源使用 `.\gradlew.bat runData` 生成后检查。
