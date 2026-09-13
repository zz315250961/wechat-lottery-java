package com.ruoyi.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

class AdminAccountScopeTest
{
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception
    {
        DataSource dataSource = new PooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:admin_account_scope;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        createSchemaAndFixtures(dataSource);
        sqlSessionFactory = createSqlSessionFactory(dataSource);
    }

    @Test
    void listReturnsOnlyUnboundNonDeletedAdminAccounts()
    {
        try (SqlSession session = sqlSessionFactory.openSession())
        {
            List<SysUser> users = session.getMapper(SysUserMapper.class).selectAdminUserList(new SysUser());
            List<String> userNames = users.stream().map(SysUser::getUserName).collect(Collectors.toList());

            assertEquals(2, users.size());
            assertTrue(userNames.contains("test-admin"));
            assertTrue(userNames.contains("test-disabled-admin"));
            assertFalse(userNames.contains("test1"));
            assertFalse(userNames.contains("test2"));
            assertFalse(userNames.contains("test3"));
            assertFalse(userNames.contains("test-deleted-admin"));
        }
    }

    private SqlSessionFactory createSqlSessionFactory(DataSource dataSource) throws Exception
    {
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.getTypeAliasRegistry().registerAlias("SysDept", SysDept.class);
        configuration.getTypeAliasRegistry().registerAlias("SysRole", SysRole.class);
        configuration.getTypeAliasRegistry().registerAlias("SysUser", SysUser.class);
        configuration.addMapper(SysUserMapper.class);
        try (InputStream mapperXml = getClass().getClassLoader().getResourceAsStream("mapper/system/SysUserMapper.xml"))
        {
            new XMLMapperBuilder(mapperXml, configuration, "mapper/system/SysUserMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void createSchemaAndFixtures(DataSource dataSource) throws Exception
    {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY, dept_id BIGINT, user_name VARCHAR(30), nick_name VARCHAR(30), email VARCHAR(50), avatar VARCHAR(100), phonenumber VARCHAR(20), sex VARCHAR(1), status VARCHAR(1), del_flag VARCHAR(1), login_ip VARCHAR(50), login_date TIMESTAMP, create_by VARCHAR(64), create_time TIMESTAMP, remark VARCHAR(500))");
            statement.execute("CREATE TABLE sys_dept (dept_id BIGINT PRIMARY KEY, parent_id BIGINT, ancestors VARCHAR(500), dept_name VARCHAR(30), order_num INT, leader VARCHAR(30), status VARCHAR(1))");
            statement.execute("CREATE TABLE sys_role (role_id BIGINT PRIMARY KEY, role_name VARCHAR(30), role_key VARCHAR(100), role_sort INT, data_scope VARCHAR(1), status VARCHAR(1))");
            statement.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
            statement.execute("CREATE TABLE lottery_merchant (id BIGINT PRIMARY KEY, sys_user_id BIGINT)");

            statement.execute("INSERT INTO sys_role VALUES (1, '管理员', 'admin', 1, '1', '0'), (3, '商户', 'merchant', 3, '3', '0'), (4, '运营', 'operator', 4, '3', '0')");
            statement.execute("INSERT INTO sys_user (user_id, user_name, nick_name, status, del_flag, create_time) VALUES (9100, 'test-admin', '测试管理员', '0', '0', CURRENT_TIMESTAMP), (9101, 'test1', '商户一', '0', '0', CURRENT_TIMESTAMP), (9102, 'test2', '商户二', '0', '0', CURRENT_TIMESTAMP), (9103, 'test3', '运营三', '0', '0', CURRENT_TIMESTAMP), (9104, 'test-disabled-admin', '停用管理员', '1', '0', CURRENT_TIMESTAMP), (9105, 'test-deleted-admin', '删除管理员', '0', '2', CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO sys_user_role VALUES (9100, 1), (9101, 1), (9101, 3), (9102, 3), (9103, 4), (9104, 1), (9105, 1)");
            statement.execute("INSERT INTO lottery_merchant VALUES (9111, 9101), (9112, 9102)");
        }
    }
}
