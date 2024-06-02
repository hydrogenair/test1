# 一、项目背景

## 1.项目需求

仓库地址：

[openGauss/examples - 码云 - 开源中国 (gitee.com)](https://gitee.com/opengauss/examples)

**兼容性问题**：由于C3P0是为各种数据库设计的通用连接池管理库，可能存在与`openGauss`数据库在使用不同驱动时的兼容性问题，导致数据库操作异常或性能下降。在实际使用过程中，可能会遇到连接池初始化失败、连接释放不及时、连接超时处理不当等问题，影响系统的稳定性和响应速度。

**性能问题**：不同驱动在连接池管理中的性能表现可能存在差异，使用MySQL驱动和`openGauss`(PG)驱动连接`openGauss`数据库时，可能会出现连接速度慢、执行SQL语句效率低等问题。

**错误处理问题**：C3P0在处理数据库操作错误时，可能存在错误信息不明确、错误恢复机制不完善等问题，导致系统难以诊断和修复问题。C3P0提供了丰富的API接口，用于配置和管理连接池，但在实际使用中，可能会遇到API接口使用不当、配置参数不合理等问题，影响连接池的正常工作。

## 2.项目相关

C3P0仓库：

https://github.com/swaldman/c3p0

**连接池管理**：

- **连接池自动增长**：C3P0可以根据需要自动增长连接池的大小，确保有足够的连接来处理应用程序的数据库请求。
- **连接池回收**：C3P0能够自动回收不再使用的连接，释放资源，避免连接泄漏。
- **连接验证**：在连接池中保持连接的有效性，确保连接在被使用前是可用的。

**配置灵活**：

- 提供丰富的配置参数，如`maxPoolSize`（最大连接池大小）、`minPoolSize`（最小连接池大小）、`initialPoolSize`（初始连接池大小）等，允许用户根据应用需求进行调整。
- 支持通过Java代码和配置文件两种方式进行配置，方便用户根据不同环境进行配置管理。

C3P0通过提供高效的数据库连接池管理，提高了Java应用程序对数据库访问的性能和可靠性。其丰富的配置参数和灵活的配置方式，使得C3P0能够适应各种复杂的应用场景，是Java开发者进行数据库连接管理的优秀选择，因此测试C3P0在不同驱动下的表现，评估其对`openGauss`数据库的支持能力，确保系统的稳定性和性能表现，对于保障Java应用在使用`openGauss`数据库时的可靠性具有重要意义。

# 二、项目方案

完整代码见：：https://github.com/hydrogenair/test1



<img src="C:\Users\吕洵\AppData\Roaming\Typora\typora-user-images\image-20240601235309341.png" alt="image-20240601235309341" style="zoom:50%;" />

## 1.数据库配置

导入jar包 `mchange-commons-java-0.2.20.jar` ` c3p0-0.9.5.5.jar`

再定义配置文件`c3p0-config.xml` 放在项目类路径下

创建核心对象 数据库连接池对象`ComboPooledDataSource`

获取连接`getConnection()`

<img src="C:\Users\吕洵\AppData\Roaming\Typora\typora-user-images\image-20240601212832432.png" alt="image-20240601212832432" style="zoom:50%;" />

## 2.设置测试用例完成设计文档

设置对于SQL多方面的测试

### DDL

```sql
-- 创建表
CREATE TABLE test_table (
    id INT PRIMARY KEY,
    name VARCHAR(50)
);
-- 修改表结构
ALTER TABLE test_table ADD COLUMN age INT;
-- 删除表
DROP TABLE test_table;
-- 创建索引
CREATE INDEX idx_name ON test_table(name);
-- 删除索引
DROP INDEX idx_name ON test_table;
```

### DML

```sql
-- 插入数据
INSERT INTO test_table (id, name, age) VALUES (1, 'LX', 19);
-- 更新数据
UPDATE test_table SET age = 20 WHERE id = 1;
-- 删除数据
DELETE FROM test_table WHERE id = 1;
-- 查询数据
SELECT * FROM test_table WHERE id = 1;
```

### DCL

```sql
-- 授予权限
GRANT SELECT ON test_table TO lx;
-- 撤销权限
REVOKE SELECT ON test_table FROM lx;
```

### 存储过程和函数

```sql
-- 创建存储过程
CREATE PROCEDURE addUser(IN uid INT, IN uname VARCHAR(50), IN uage INT)
BEGIN
    INSERT INTO test_table (id, name, age) VALUES (uid, uname, uage);
END;
-- 调用存储过程
CALL addUser(4, 'yjh', 30);
-- 删除存储过程
DROP PROCEDURE addUser;

-- 创建函数
CREATE FUNCTION getAge(uid INT) RETURNS INT
BEGIN
    DECLARE uage INT;
    SELECT age INTO uage FROM test_table WHERE id = uid;
    RETURN uage;
END;
-- 调用函数
SELECT getAge(4);
-- 删除函数
DROP FUNCTION getAge;

```

### 边界条件测试

```sql
-- 大数据量插入
INSERT INTO test_table (id, name, age) 
SELECT generate_series(1, 1000000), 'Name' || generate_series(1, 1000000), 20;
```

## 3.使用JUnit进行测试

```java
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class C3P0UnitTest {
    private ComboPooledDataSource cpds;

    @Before
    public void setUp() {
        cpds = new ComboPooledDataSource("default-config");
    }

    @After
    public void tearDown() {
        DataSources.destroy(cpds);
    }

    @Test
    public void testDatabaseOperations() throws SQLException {
        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // DDL
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id INT PRIMARY KEY, name VARCHAR(50), age INT)");

            // DML
            stmt.executeUpdate("INSERT INTO test_table (id, name, age) VALUES (1, 'lx', 19)");
            stmt.executeUpdate("UPDATE test_table SET age = 20 WHERE id = 1");
            ResultSet rs = stmt.executeQuery("SELECT * FROM test_table WHERE id = 1");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("lx", rs.getString("name"));
            assertEquals(20, rs.getInt("age"));
            stmt.executeUpdate("DELETE FROM test_table WHERE id = 1");
			...

        }
    }
}

```

## 4.撰写测试报告

```txt
一、测试概述
项目名称：C3P0连接池和数据库操作测试
测试日期：2024年6月X日
二、测试环境
硬件环境：
...
软件环境：
操作系统：Windows 11
JDK版本：OpenJDK 11
数据库：openGauss 2.0.0 
连接池框架：C3P0 0.9.5.5
JDBC驱动：PostgreSQL JDBC 42.2.24 / MySQL Connector/J 8.0.26
三、测试用例设计
测试用例1：DDL语句测试
描述：验证创建、修改和删除表的DDL操作。
输入：CREATE TABLE、ALTER TABLE、DROP TABLE语句
...
预期输出：成功执行DDL语句，无错误。
结果：通过
...
四、测试结果总结
五、发现的问题和解决方案
六、结论和建议
```

# 三、技术方法&可行性

## 1.技术方法

### 数据库

**JDBC驱动配置**：

- MySQL JDBC驱动和`openGauss`(PG) JDBC驱动的安装和配置。
- 驱动的加载和初始化。

**C3P0连接池配置**：

- 初始化C3P0连接池。
- 设置连接池的各种配置参数，如`maxPoolSize`、`minPoolSize`、`initialPoolSize`等。

**SQL语法**：

- **DDL**：CREATE TABLE、ALTER TABLE、DROP TABLE等。
- **DML**：INSERT、UPDATE、DELETE、SELECT等。
- **DCL**：GRANT、REVOKE等。
- **存储过程**：创建、调用、删除存储过程。

### 测试和优化

**性能测试工具**：

- 使用JMeter或其他性能测试工具进行数据库连接和操作的性能测试。
- 分析连接池的性能指标，如连接获取时间、SQL执行时间等。

**性能优化**：

- 调整C3P0连接池的配置参数，优化连接池性能。
- 优化SQL语句和数据库操作，提高执行效率。

**测试用例执行**：

- 编写JUnit测试类，执行设计好的测试用例。
- 记录测试结果，并分析测试中遇到的问题。

本人在校做《智汇语研》项目时做过压力测试，其中包括对web端、服务端以及模型性能测试，对软件测试有一定基础。

## 2.可行性

本人在多个项目中担任后端开发角色，积累了丰富的项目开发经验，熟悉后端技术栈和开发流程。参与华为鲲鹏&昇腾原生人才高校促进计划，深入学习了相关技术，熟练使用`openGauss`数据库，掌握其架构、特性和优化技巧，能够高效地进行数据库管理和操作。通过自学和项目实践，已经掌握了C3P0连接池管理库的基本使用方法，能够在项目中进行数据库连接池的配置和管理。

在校期间参与《智汇语研》项目，负责进行压力测试，包括对web端、服务端以及模型性能的测试。具备一定的软件测试基础，能够设计和执行测试用例，分析测试结果并提出优化建议。

# 四、项目规划

目前本人是本科大二在读，且有多次项目开发经验，可以保证暑假有充足的时间参与开源项目的开发，希望能够得到这次锻炼的机会，探索更多后端、数据库方向更多有意思的挑战。

项目第一阶段 （7.1-8.15）

- [ ] 熟悉项目架构和技术栈
- [ ] 深度学习openGuas相关
- [ ] 学习软件测试相关标准流程
- [ ] 参考专业测试完成测试设计文档
- [ ] 接入C3P0框架，使用MySQL驱动和`openGaus`s(PG)驱动连接`openGauss`

项目第二阶段（8.16-9.30）

- [ ] 完成初步测试
- [ ] 解决中期验收中发现的问题
- [ ] 进一步完善测试
- [ ] 完成测试报告
- [ ] 进行项目总结
