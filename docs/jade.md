# Jade 集成

Jade 是可选依赖，当前按 NeoForge 26.3.5 API 编译；Diversity Core 不包含 Jade。开发客户端的 run/mods 中需放入对应版本的 Jade。

## 接入其他项目

让方块或方块实体实现公共接口 `net.aquiferic.diversitycore.api.energy.EnergyBarProvider` 即可，无需额外注册 Jade 插件，也无需在实现类中导入 Jade API。

普通储能机器通常在方块实体中实现：

```java
@Override
public EnergyBarData getEnergyBar(Level level, BlockPos pos, BlockState state) {
    return EnergyBarData.of(energy.getAmountAsLong(), energy.getCapacityAsLong());
}
```

将示例中的 energy 替换为机器实际的能量存储。容量必须大于零，储能不能为负；暂时没有储能时返回 null，保留 Jade 默认显示。能量值支持 long，超出容量时填充比例限制为 100%。

创造电能库在方块上实现接口，返回：

```java
return EnergyBarData.infinite(
    Component.translatable("jade.diversitycore.infinite_energy"));
```

自定义文字可使用 `new EnergyBarData(stored, capacity, Component.translatable("yourmod.energy", ...))`。实现同时存在于方块与方块实体时，以方块实体为准；其返回 null 时不会再查询方块。

接口在服务端调用，数值与文字组件由 Jade 同步到客户端，文字按客户端语言解析。多人游戏需要服务端和客户端都安装 Jade 才会显示自定义能量条；缺少同步数据时保留默认显示。公共接口不依赖 Jade，未安装 Jade 时依然可以正常加载模组。

Jade 的 Diversity Core 分类中，“Diversity 风格能量条”提供三种状态：

- **全部**：优先采用接口数据；没有接口数据时，将 Jade 通用能量接口已同步的各个储能值显示为 Diversity 风格能量条，并保留每组能量的单位。其他模组无需实现本模组接口，但必须将能量接入 Jade 的通用能量接口。
- **仅 Diversity 模组**（默认）：仅显示 EnergyBarProvider 接口提供的数据，其他方块保持原样。这里按接口判断，不按模组 ID 判断。
- **关闭**：保留原来的 Jade 显示。

三种状态都遵循 Jade 的能量显示开关和仅显示详情设置。“全部”会替换该方块的能量显示，包括 Jade 原本的纯文本或图标样式。没有可用能量数据时不会移除原显示。
