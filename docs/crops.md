# 作物注册 API

在模组构造阶段声明作物，最后与其他内容一起调用 `CONTENT.register(bus)`。实际方块和物品由 NeoForge 的注册事件创建。

## 普通作物

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

掉落通过原生 loot table 处理，并应用爆炸衰减。创造破坏整株不会触发另一半的额外掉落。此次 API 不包含右键收获。

## 验证

```text
gradlew check verifyCropResources
gradlew runCropRenderVerification
```

第一条验证声明契约、生成资源和真实服务器 GameTest；第二条启动独立验证客户端，在资源加载后检查作物全部状态的实际烘焙几何和 CUTOUT 渲染层，然后自动退出。测试作物与生成资源仅属于 verification source set，不会进入正式模组 jar。
