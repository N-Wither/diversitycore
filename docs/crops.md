# 作物注册 API

在模组构造阶段声明作物，最后与其他内容一起调用 `CONTENT.register(bus)`。实际方块和物品由 NeoForge 的注册事件创建。

## 原版简单作物

`createSimple(id)` 创建直接继承原版 `CropBlock` 的单格作物，固定 8 个阶段（age 0–7）。
沿用原版耕地、生长、骨粉、选中框和种子拾取行为，Jade 可直接识别原版年龄属性。

```java
public static final SimpleCropEntry CABBAGE = CONTENT.crops().createSimple("cabbage")
    .displayName("en_us", "Cabbage")
    .model(CropModelType.CROSS)
    .seed(seed -> seed.displayName("en_us", "Cabbage Seeds"))
    .produce(ModItems.CABBAGE)
    .register();
```

返回的 `SimpleCropBuilder` 支持名称、纹理、模型、标签、种子/产物和掉落表配置，不提供 `stages()`、
两格、浅水、基底、生存/生长条件或自定义方块属性等特殊功能。`stageTextures()` 必须提供 8 张纹理；
默认路径与下文相同。默认生成独立种子和产物，也支持引用已有物品；已有种子仍需正确种植该作物。
默认掉落为保底 1 粒种子，成熟额外 1 个产物及原版小麦概率的奖励种子（含时运）；可通过 `loot()` 替换。

需要马铃薯式的产物自种时，可使用 `selfSeeded()`：

```java
public static final SimpleCropEntry GARLIC = CONTENT.crops().createSimple("garlic")
    .displayName("en_us", "Garlic")
    .selfSeeded()
    .produce(item -> item.displayName("en_us", "Garlic"))
    .loot(CropLootTables.crop().seeds(1).produce(1, 3, true).bonusSeeds(0).build())
    .register();
```

此时只生成 `garlic` 物品，`seed()` 与 `produce()` 指向同一可种植物品，不生成 `garlic_seeds`。
食物等属性在 `produce(...)` 回调里配置；不能再设置独立种子或引用已有产物。
不能再单独注册同名普通物品，会导致 ID 冲突。`seeds(1)` 保底掉落和成熟的 `produce(...)` 数量会相加。

`SimpleCropEntry` 提供 `block()`、`seed()`、`produce()` 延迟引用，没有上部方块。
元数据从 `CONTENT.simpleCropDefinitions()` 读取，现有模型、掉落、标签和语言 provider 已支持。
下游项目发现声明语言时，也应遍历 `simpleCropDefinitions()` 中的 `names()`。

## 可配置作物

```java
public static final CropEntry WHEAT = CONTENT.crops().create("example_wheat")
    .stages(8)
    .displayName("zh_cn", "示例小麦")
    .seed(seed -> seed.displayName("zh_cn", "示例小麦种子"))
    .produce(produce -> produce.displayName("zh_cn", "示例小麦"))
    .register();
```

默认创建方块 `example_wheat`、可种植的物品 `example_wheat_seeds` 和产物物品 `example_wheat`。植株没有额外的普通方块物品。

`CropEntry.block()`、`upperBlock()` 返回延迟方块引用；`seed()`、`produce()` 返回物品 Supplier。不要在声明阶段调用这些引用的 `get()`。

种子和产物回调使用现有 `ItemBuilder<Item>`，可配置物品属性、贴图、标签和翻译。回调中的 builder 由作物统一提交，不能自行调用 `register()`，提交作物后也不可继续修改。

## 两格作物

```java
var hemp = CONTENT.crops().create("example_hemp")
    .stages(5)
    .tall(HarvestMode.REGENERATE)
    .register();

var rice = CONTENT.crops().create("example_rice")
    .stages(4)
    .tall(HarvestMode.REGENERATE)
    .upperStages(4)
    .shallowWater()
    .substrate(BlockTags.DIRT)
    .selfSeeded()
    .produce(item -> item.displayName("zh_cn", "示例稻谷"))
    .register();
```

两格作物额外注册 `<id>_top` 方块。下部阶段数为 2–16，默认 8；上部为 1–16，默认 1，即长出即成熟。下部成熟后，下一次有效自然生长或施肥会在上方生成上部。上方必须为空气且不能超出建筑高度。

- `REGENERATE`：打掉上部留下成熟根部，之后可重新长出上部；打掉根部会一起移除上部。
- `WHOLE_PLANT`：打掉任意部分都会移除整株，并分别结算两部分的掉落。

独立生长的上部从第 0 阶段开始。对根部施肥会转交给尚未成熟的上部，也可以直接对上部施肥。完全成熟或空间受阻时骨粉不生效。自然生长遵循 NeoForge 的作物生长前后事件。

## 基底与环境

以下两种写法等价，字符串参数表示方块标签，不是单个方块 ID：

```java
.substrate("minecraft:sand")
.substrate(BlockTags.SAND)
```

也可以使用自己声明的 `TagKey<Block>`。字符串必须包含命名空间且不带 `#`；标签目标注册表和 ID 都会经过现有 `QS.blt()` 校验。基底标签的成员由数据包提供，可通过数据包调整。

重复调用 `substrate()` 时最后一次生效。显式基底覆盖预设默认值，不受 `shallowWater()` 调用顺序影响。未指定基底的陆生作物使用原版作物基底规则，并尊重 NeoForge 的土壤支持判断；显式基底按所选标签决定是否支持。

`shallowWater()` 要求根部在水源中且上方没有水，默认基底为 `BlockTags.DIRT`。种子可对浅水底部的有效基底使用。流水和干地不接受种植，种植失败不消耗种子。根部被移除时恢复水源；用桶抽走根部水源会使植物失效。

```java
.survivalCondition((level, rootPos, rootState) -> /* 额外生存条件 */ true)
.growthCondition((level, rootPos, rootState) -> /* 额外生长条件 */ true)
```

条件回调始终接收根部位置与状态，并与基底、水体和光照条件共同判断。生存条件在放置和邻居状态更新时检查；生长条件控制自然生长和骨粉，不影响植株存在。回调应只读、可在客户端安全执行，不应主动加载区块。

## 复用已有物品

```java
.produce(ModItems.FIBER) // Supplier<? extends Item>，不生成新的产物物品或其资源
.seed(ModItems.EXISTING_SEEDS)
```

已有种子必须是种植本作物下部的 `BlockItem`，在注册完成后的 common setup 阶段校验。普通物品不会自动获得种植行为；自定义种子还应自行正确处理水中放置等规则。若需要前向引用，可以在物品工厂中通过 Supplier 引用之后声明的 `CropEntry`。

`selfSeeded()` 让自动生成的产物同时作为种子，不生成 `<id>_seeds`。使用 `produce(...)` 回调配置它，不能同时设置独立种子或引用外部产物。种子及产物回调与对应的外部物品引用不能混用。

## 资源与掉落

植株默认使用小麦式的四平面模型（`CropModelType.CROP`）。调用
`.model(CropModelType.CROSS)` 可改用草式的双平面交叉模型，不附加草的生物群系染色。
此设置适用于所有生长阶段，以及两格作物的上下两部分；种子和产物的物品模型不受影响。

```java
CONTENT.crops().create("cabbage")
    .stages(8)
    .model(CropModelType.CROSS)
    .register();
```

### 自定义贴图比例

`create()` 和 `createSimple()` 都支持 `.textureRatio(width, height)`，默认 `(1, 1)`。
参数为正整数比例，例如 `(1, 2)` 与 `(16, 32)` 等价。
模型保持原有水平尺寸和底部位置，高度为 `16 * height / width` 模型单位。
因此单格作物使用 16×32 的阶段贴图时，视觉高度为两格；生长、选中框和碰撞不变。
适用于 CROP、CROSS 和两格作物的上下部分，物品模型不受影响。

```java
CONTENT.crops().createSimple("tomato")
    .textureRatio(1, 2)
    .model(CropModelType.CROSS)
    .register();
```

每阶段图片本身应采用声明的比例。修改比例后需要重新运行 `runData` 生成模型。

默认贴图路径（N 从 0 开始）：

| 用途 | 路径 |
| --- | --- |
| 下部各阶段 | `<namespace>:block/<id>_stageN` |
| 上部各阶段 | `<namespace>:block/<id>_top_stageN` |
| 种子 | `<namespace>:item/<id>_seeds` |
| 产物 | `<namespace>:item/<id>` |

`texture(...)` 更改植株贴图前缀；`stageTextures(String...)`、`upperTextures(String...)` 可以逐年龄指定贴图，数量必须与对应阶段数一致，重复贴图会共享模型。种子与产物贴图通过各自回调设置。

复用现有 `ModModelProvider`、`ModLanguageProvider`、`ModLootProvider`、`ModBlockTagsProvider`、`ModItemTagsProvider`。Diversity Core 自己的 datagen 已接入；使用独立 ContentRegistry 的模组应将其传入这些 provider，并为声明的语言注册对应语言 provider。

`tag(String|TagKey<Block>)` 为上下两部分声明方块标签成员；`substrate(...)` 只选择种植基底，不会修改基底标签。种子和产物的物品标签通过各自回调声明。作物会自动加入 `minecraft:crops`，根部加入 `minecraft:maintains_farmland`；陆生根部和所有上部加入 `minecraft:washed_away_by_fluids`，保持原版耕地和流水行为。

单格作物默认返还一粒种子，成熟时额外掉落一个产物和原版小麦概率的奖励种子。两格作物由下部掉落保底种子、成熟上部掉落产物和奖励种子；只收上部不会额外返还根部的保底种子。自种产物使用同一物品。

- `produceCount(int)`：成熟产物数量，默认 1。
- `bonusSeedTrials(int)`：奖励种子的二项分布次数，默认 3，概率为 0.5714286，包含时运加成；设为 0 禁用奖励种子。

掉落通过原生 loot table 处理，并应用爆炸衰减。创造破坏整株不会触发另一半的额外掉落。

本模组不包含右键收获功能。

## 自定义掉落表

`create()` 和 `createSimple()` 都支持 `.loot(CropLootFactory)`，使用
`net.aquiferic.diversitycore.api.loot` 包中的接口和工厂。不调用时保持上述默认掉落。
调用后完整替换默认表；`produceCount()` 和 `bonusSeedTrials()` 不再参与生成，不受调用顺序影响。
重复调用 `loot()` 时最后一次生效，提交注册后不可修改。

```java
CONTENT.crops().createSimple("tomato")
    .produce(ModItems.TOMATO)
    .loot(CropLootTables.crop()
        .produce(2, 4)  // 成熟收获部位均匀随机掉落 2–4 个产物，包含两端
        .seeds(1)      // 任意年龄的根部固定返还 1 粒种子
        .bonusSeeds(0) // 禁用成熟奖励种子
        .build())
    .register();
```

工厂默认 `produce(1)`、`seeds(1)`、`bonusSeeds(3)`，与原有默认规则一致。
`produce(count)` / `seeds(count)` 设置固定数量，双参数重载设置包含两端的均匀随机数量。
产物可通过 `produce(count, boolean fortune)` 或 `produce(min, max, boolean fortune)`
控制是否受时运影响；省略布尔参数等同于 `false`。
例如 `produce(2, 4, true)` 先随机取 2–4，再按原生 `uniform_bonus_count` 规则增加
0 到时运等级个产物（时运 III 时总量为 2–7）。没有时运或没有工具时保持基础数量。
时运仅作用于成熟产物，不改变基础种子、奖励种子或成熟条件；`produce(0, true)` 仍关闭产物掉落。
再次调用任何 `produce(...)` 重载都会替换数量和开关，旧重载会将开关重置为 `false`。
数量范围为 0 到 `Item.ABSOLUTE_MAX_STACK_SIZE`；0 可关闭对应基础掉落。
`seeds(0)` 不会关闭独立的成熟奖励种子，需另设 `bonusSeeds(0)`；奖励种子保留原版概率和时运加成。
`build()` 对当前配置取快照，可复用到不同作物；之后修改工厂 builder 不会影响已构建的工厂。
工厂处理单格及两格作物，并保留爆炸衰减；`CropLootTables.empty()` 可令植株无掉落。

复杂规则可直接返回新的原生 `LootTable.Builder`，例如任意年龄掉落钻石：

```java
.loot(context -> LootTable.lootTable()
    .withPool(LootPool.lootPool()
        .setRolls(ContextIntProviders.exactly(1))
        .add(LootItem.lootTableItem(Items.DIAMOND))))
```

回调仅在 `runData` 时执行，每个方块部位执行一次，应每次返回新的非 null builder。
`CropLootContext` 提供当前方块、种子、产物、注册表查询、年龄属性、最大年龄和 `tall` / `upper` 标志。
`mature()` 创建当前部位的成熟条件；没有年龄属性的上部视为成熟。
`seedPart()` 表示应产出基础种子的根部，`harvestPart()` 表示单格植株或两格作物的上部。
原生回调不会自动追加默认种子、成熟条件或爆炸函数；这些由回调自行配置。
provider 将表类型设为 `BLOCK`，并按当前方块的掉落表 ID 输出到 `src/generated/resources`。
修改声明后先运行 `runData`，再构建；不要手动修改生成的 JSON。

## 验证

```text
gradlew check verifyCropResources
gradlew runCropRenderVerification
```

第一条验证声明契约、生成资源和真实服务器 GameTest；第二条启动独立验证客户端，在资源加载后检查作物全部状态的实际烘焙几何和 CUTOUT 渲染层，然后自动退出。测试作物与生成资源仅属于 verification source set，不会进入正式模组 jar。
