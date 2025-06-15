## 步骤1
在该空目录下创建一个标准maven java工程，多模块
我的运行环境是Windows，我经常用的shell 是powershell。先不要创建子模块，创建一个空的父模块架子即可。
我使用IDEA开发，不使用mvn进行工程创建，另外java版本为1.8
	
## 步骤2
在该工程下创建一个子模块，并在该子模块下写java代码用于除去一个java maven 工程中一些我不想要的文件夹或者文件，
我需要对这个java工程进行文件剔除，其路径为"K:\tender-mgt"
具体内容/步骤如下
a. 创建输出文件夹output(如果存在就删除,然后再创建)，并在文件夹中创建 step1-unsed-set  和 step2-set，这两个文件夹也是(如果存在就删除,然后再创建)，将需要剔除的文件copy到 step1-unsed-set，将不需要剔除的文件copy到step2-set文件


b. 剔除 .git 文件夹；剔除 *.java, *.xml 之外的所有文件，但保留根目录下的文件以及不动resource文件夹下的所有文件

c. 剔除后发现是空的文件夹，直接删除

d. 把删除的文件以目录树的形式打印出来

对于该子模块代码结构的说明，可以加一个配置类，用于设置剔除规则，简单一点的，只取必要配置。然后getter和setter可以用lombok插件

### 优化1
1. log.xx 使用slf4j 日志系统，不要用System.out.println()
2. try catch 粒度细一点，不要包在最外面，Exception 类型指定需要明确
3. 不要用常量字符串，用常量枚举
4. 删除文件夹的方法deleteDirectory有问题，在删除.git文件夹下面的某个文件时出问题了。
另外，对于接下来创建的两个子目录也要这么判断(如果存在就删除，不存在就创建)

## 步骤三
创建一个新的子模块，用于处理 output\step2-set 这个文件夹(java多模块项目+springcloud+mybatis)中的代码。
我先介绍一下这个项目，然后给出我得需求。看完之后不用着急给我写代码，先给我一个大概的设计，让我先听明白
这个项目tender-api和tender-job中得方法是可以直接使用的，比如job中的可以当作定时任务使用，api中的方法可以作为api接口被前端调用；tender-contract也是接口，但是是提供于tender-api和tender-job这两个模块中的接口使用的。剩下的tender-business模块中的方法都是被调用的方法，被tender-api和tender-job中的方法所使用。

1. 找到该项目子模块tender-api中的API接口方法
2. 找到该方法所有的依赖，你需要确定该方法依赖哪些东西，并将这些依赖分类
3. 希望能够设置依赖的深度和广度，其中深度是指方法A依赖了B、C和注解D，则B、C、D就是一级引用，而B、C中又依赖了E和F，E和F就是二级引用，依次类推。然后广度是指去搜寻依赖的范围，设置一个基本依赖范围(调用的方法，参数，注解，常量)，然后拓宽，比如增加考虑范围，考虑方法使用的AOP和IOC(spring特色)，考虑使用的配置，考虑使用的mapper依赖的xml(mybatis特色)等等
4. 确定输入和输出，主要是输出，你打算怎么输出依赖结果给我看，输出需要整洁详细

### 设计建议和补充1
循环依赖检测与处理，动态调用(比如使用了反射)的静态分析，如何实现多层递归分析，不包含外部依赖(第三方库等)，额外优化。使用策略模式、工厂模式、访问者模式使代码更加清晰。
对基本依赖范围进行分类：
- 声明型依赖 ：方法签名中的依赖（注解、参数、返回类型、异常声明）
- 执行型依赖 ：方法体中的依赖（方法调用、字段访问、异常处理、对象创建）
简化，不用给我代码，给我思路

### 设计建议和补充2
输出可以优化一下，每个依赖添加所在的类，以及代码位置

由于扫描整个项目过于复杂(该项目有55万行代码，不包括注释)，你可以先创建一个模块指定扫描范围为`output\step2-set\tender-api\src\main\java\com\paut\tender\mgt\api\controller\order\PurchaserOrderSettlementController.java`这个文件。然后目标方法为：
```java
    /**
     * 查询结算单列表
     */
    @ApiOperation(value = "查询结算单list", notes = "查询结算单list")
    @PostMapping("/search")
    @Auth({PurchaserAuthConstant.TENDER_PURCHASER_ORDER_SETTLEMENT_LIST, PurchaserAuthConstant.TENDER_PURCHASER_ORDER_SETTLEMENT_LIST_TOC})
    public PageResponse<OrderSettlementEntity> search(@Validated @RequestBody OrderSettlementSearchEntity searchEntity);
```
但是需要考虑可扩展性，后面扫描范围会提升至扫描整个项目
现在开始做吧，输入需要指定类以及指定的方法

### 代码优化3 - xxx.aaa 和 xxx 合并
我运行了代码之后有下面这样的输出：
  📁 方法调用: PageUtil.process [279:16-281:90]
    ├── 📄 类: com.paut.tender.mgt.api.controller.order.PurchaserOrderSettlementController
    ├── 📂 文件: K:\power-script\core-extract\output\step2-set\tender-api\src\main\java\com\paut\tender\mgt\api\controller\order\PurchaserOrderSettlementController.java
    └── 📝 描述: 方法调用: PageUtil.process(searchEntity, () -> orderSettlementService.searchOrderSettlementList(searchEntity), () -> orderSettlementService.searchSettlementListCountTotal(searchEntity))
  📁 静态访问: PageUtil [279:16-279:23]
    ├── 📄 类: com.paut.tender.mgt.api.controller.order.PurchaserOrderSettlementController
    ├── 📂 文件: K:\power-script\core-extract\output\step2-set\tender-api\src\main\java\com\paut\tender\mgt\api\controller\order\PurchaserOrderSettlementController.java
    └── 📝 描述: 静态类访问: PageUtil
这样应该是PurchaserOrderSettlementController.java#search方法体内的`return PageUtil.process`这个代码生成的输出，输出了方法调用: PageUtil.process和静态访问: PageUtil。我觉得两者取一个精确的就能表达出PurchaserOrderSettlementController.java#search方法对于这个依赖的信息，这样就冗余了。
所以你应该新增一个策略，如果遇到了 XXX.aaa 和 XXX 这种依赖，应该把XXX去掉。你可以把收集到一个map中，这个map用于存类，比如遇到了XXX这个依赖了，就创建一个条目key = XXX，值为数组，装入XXX。如果后面遇到精确的XXX.aaa这种，就去掉XXX。如果先遇到XXX.aaa，再遇到XXX，就不添加。

### 代码优化4
1. 我发现 PurchaserAuthConstant.TENDER_PURCHASER_ORDER_SETTLEMENT_LIST 这种只要出自不在本文件
类就会是这样 PurchaserAuthConstant ，但是我需要完整的类路径。
2. 另外，对于注解依赖使用getClassName方法无法获取其所在的接口名或者类名
3. 另外不希望通过推测路径这种方式去得到出自的文件路径