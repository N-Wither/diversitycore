# 物品配方 API

在 Java 中使用链式 API 声明配方，再通过 datagen 生成 Minecraft 26.3 的配方和解锁进度 JSON。

## 在本项目中声明配方

在 `ModRecipes.init()` 中使用 `ModRegistries.RECIPES`。项目已经安排好初始化、冻结和 Provider 挂载；执行 `.\gradlew.bat runData` 生成正式资源。

```java
import static net.aquiferic.diversitycore.registry.ModRegistries.RECIPES;

static void init() {
    RECIPES.shaped("wrench", ModItems.WRENCH)
        .pattern(" II", " SI", "S  ")
        .define('I', "#c:ingots/iron")
        .define('S', Items.STICK)
        .category(RecipeCategory.TOOLS)
        .register();
}
```

所有入口的第一个参数是当前注册表命名空间内的配方路径，例如 `tools/wrench`。不接受带命名空间或包含 `..` 的路径。同一注册表的所有类型共享配方 ID，重复提交会报错。

一个物品可以有多个配方，为它们使用不同路径即可，例如 `iron_from_smelting` 和 `iron_from_blasting`。

## 原料与产物

以下四种原料写法适用于 `define`、`requires`、`input`、`template`、`base` 和 `addition`：

| 类型 | 示例 |
| --- | --- |
| 物品或有物品形式的方块 `ItemLike` | `Items.STICK`、`Blocks.STONE`、`ModItems.WRENCH` |
| 物品标签 `TagKey<Item>` | `ItemTags.PLANKS`、`QS.itt("c:ingots/iron")` |
| 原生 `Ingredient` | `Ingredient.of(Items.COAL, Items.CHARCOAL)` |
| 字符串 | `"minecraft:stick"`、`"#minecraft:planks"` |

字符串必须写完整命名空间。标签字符串的 `#` 前缀只用于原料；结果不能是标签。方块标签不能作为物品原料。

固定产物支持 `ItemLike`（包括 `DeferredItem`）和完整物品 ID 字符串；省略 `.count(n)` 时产出一个。数量遵循 26.3 原生物品模板的 1–99 范围；应根据实际物品堆叠上限选择可用数量。

声明时不会调用延迟引用的 `.get()`。字符串、标签和延迟引用在 datagen 阶段解析，缺失物品会产生包含配方 ID 的错误。标签保持标签引用，不会展开为固定物品列表；标签成员仍由数据包决定。

## 有序与无序合成

有序合成使用 1–3 行、每行 1–3 个字符的等宽图案；空格代表空格子。图案中的非空格字符必须与 `define` 定义一一对应，不能遗漏、重复定义或定义未使用字符。

```java
RECIPES.shaped("sticks", Items.STICK)
    .pattern("P", "P")
    .define('P', ItemTags.PLANKS)
    .count(4)
    .group("sticks")
    .register();

RECIPES.shapeless("flint_from_gravel", "minecraft:flint")
    .requires("minecraft:gravel", 3)
    .register();
```

`requires(ingredient, n)` 表示需要 n 个合成格；不是单个格子中的堆叠数量。所有原料合计必须占用 1–9 格。

## 加工与切石

```java
RECIPES.smelting("iron_from_smelting", Items.IRON_INGOT)
    .input(Items.RAW_IRON)
    .experience(0.7F)
    .cookingTime(200)
    .register();

RECIPES.blasting("iron_from_blasting", Items.IRON_INGOT)
    .input(Items.RAW_IRON)
    .experience(0.7F)
    .register();

RECIPES.smoking("cooked_beef_from_smoking", Items.COOKED_BEEF)
    .input(Items.BEEF)
    .register();

RECIPES.campfireCooking("cooked_beef_from_campfire", Items.COOKED_BEEF)
    .input(Items.BEEF)
    .register();

RECIPES.stonecutting("stone_slabs", Items.STONE_SLAB)
    .input(Items.STONE)
    .count(2)
    .category(RecipeCategory.BUILDING_BLOCKS)
    .register();
```

| 类型 | 默认时间（tick） | 默认经验 |
| --- | --- | --- |
| 熔炉 `smelting` | 200 | 0 |
| 高炉 `blasting` | 100 | 0 |
| 烟熏炉 `smoking` | 100 | 0 |
| 营火 `campfireCooking` | 600 | 0 |

加工时间必须为正整数，经验必须为有限的非负数。熔炉和高炉默认使用 `CookingBookCategory.MISC`，可通过 `.cookingCategory(...)` 修改；烟熏炉和营火使用原生固定的 `FOOD` 分类。

## 锻造升级与纹饰

```java
RECIPES.smithingTransform("netherite_axe", Items.NETHERITE_AXE)
    .template(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
    .base(Items.DIAMOND_AXE)
    .addition(Items.NETHERITE_INGOT)
    .category(RecipeCategory.TOOLS)
    .register();

RECIPES.smithingTrim("sentry_trim", TrimPatterns.SENTRY)
    .template(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE)
    .base("#minecraft:trimmable_armor")
    .addition("#minecraft:trim_materials")
    .register();
```

两类锻造都要求提供模板、基底和添加物。纹饰入口接收 `ResourceKey<TrimPattern>`，按原生规则修改输入装备，没有固定产物和数量设置。锻造配方不提供原生类型不支持的 `group`。

## 配方书解锁

默认分类为 `RecipeCategory.MISC`。默认自动为原料生成持有条件，玩家拥有任意一种原料即可解锁；原料包含标签或普通多选 `Ingredient` 时，持有其中任意一种匹配物品即可。生成的进度采用原生 OR 语义，也包含原生的配方已解锁条件。

`.unlockedBy(name, criterion)` 完全替换自动条件；重复调用时各条件之间为 OR。名称必须非空、唯一，且不能使用原生保留名称 `has_the_recipe`。

```java
RECIPES.shapeless("manual_unlock", Items.FLINT)
    .requires(Items.GRAVEL)
    .unlockedBy("custom_condition", myCriterion)
    .register();

RECIPES.shapeless("without_advancement", Items.FLINT)
    .requires(Items.GRAVEL)
    .noAdvancement()
    .register();
```

`noAdvancement()` 只输出配方，不生成解锁进度。该配方仍然存在，可以按游戏规则合成；配方书发现方式由调用方负责。它不能与显式解锁条件混用。

NeoForge 自定义 `Ingredient` 可能包含组件等复杂匹配规则，无法通用地转换为等价的持有条件，因此必须显式设置 `unlockedBy` 或 `noAdvancement`。

## 在其他模组中使用

从 `net.aquiferic.diversitycore.api.recipe` 导入 `RecipeRegistry`，从 `net.aquiferic.diversitycore.datagen` 导入 `ModRecipeProvider`。原版常量和类型使用对应的 Minecraft 包。

创建独立的注册表，先声明配方，再冻结。物品注册与配方声明的生命周期分别管理：

```java
public static final RecipeRegistry RECIPES = new RecipeRegistry("your_mod");

// 在模组构造期间，声明完所有配方后调用：
RECIPES.freeze();
```

在你自己的 `GatherDataEvent.Server` 或 `GatherDataEvent.Client` 监听器中挂载一次 Provider：

```java
event.addProvider(new ModRecipeProvider(
    event.getGenerator().getPackOutput(),
    RECIPES,
    event.getWorldLookupProvider(),
    event.getReloadableLookupProvider()
));
```

Provider 使用 26.3 的原生多注册表 bootstrap 和 codec，输出 `data/your_mod/recipe/<path>.json` 与 `data/your_mod/advancement/recipes/<category>/<path>.json`。

每个 Builder 只能 `register()` 一次，提交后不可修改；`freeze()` 后不能创建、修改或提交 Builder。重复冻结无副作用，已冻结的声明可以重复用于 datagen。Provider 要求注册表已经冻结。

## 验证

执行 `.\gradlew.bat verifyRecipeApi` 检查声明生命周期、延迟引用、全部配方类型、解锁设置和生成 JSON 的原生 codec 回读。该验证也已纳入 `check`。验证资源输出到 `build/verification-data`，不打包进正式模组。
