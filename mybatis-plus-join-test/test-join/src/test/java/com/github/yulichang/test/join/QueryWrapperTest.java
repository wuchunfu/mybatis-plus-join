package com.github.yulichang.test.join;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.query.MPJQueryWrapper;
import com.github.yulichang.test.join.dto.UserDTO;
import com.github.yulichang.test.join.entity.UserDO;
import com.github.yulichang.test.join.entity.UserTenantDO;
import com.github.yulichang.test.join.mapper.UserMapper;
import com.github.yulichang.test.join.mapper.UserTenantMapper;
import com.github.yulichang.test.util.Reset;
import com.github.yulichang.test.util.ThreadLocalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class QueryWrapperTest {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTenantMapper userTenantMapper;


    @BeforeEach
    void setUp() {
        Reset.reset();
    }

    /**
     * 链表查询
     */
    @Test
    void table() {
        ThreadLocalUtils.set("SELECT t.id, t.pid, t.`name`, t.`json`, t.sex, t.head_img, t.create_time, t.address_id, " +
                "t.address_id2, t.del, t.create_by, t.update_by, `name` AS nameName FROM (SELECT * FROM `user`) t " +
                "WHERE t.del = false AND t.id = 1");
        MPJQueryWrapper<UserDO> wrapper = new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .setTableName(name -> String.format("(select * from %s)", name))
                .select("`name` AS nameName")
                .last("AND t.id = 1");

        userMapper.selectJoinOne(UserDTO.class, wrapper);
    }

    @Test
    void test2() {
        List<UserDO> userDO = userMapper.selectJoinList(UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id", 10));
        System.out.println(userDO);

        List<UserDTO> dto = userMapper.selectJoinList(UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id", 10));
        System.out.println(dto);
    }

    @Test
    void test3() {
        IPage<UserDO> userDO = userMapper.selectJoinPage(new Page<>(1, 10), UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(userDO);

        IPage<UserDTO> dto = userMapper.selectJoinPage(new Page<>(1, 10), UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(dto);
    }

    @Test
    void test4() {
        List<Map<String, Object>> maps = userMapper.selectJoinMaps(new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(maps);

        List<Map<String, Object>> joinMaps = userMapper.selectJoinMaps(new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(joinMaps);
    }

    @Test
    void test5() {
        ThreadLocalUtils.set("SELECT t.id, t.pid, t.`name`, t.`json`, t.sex, t.head_img, t.create_time, t.address_id, " +
                "t.address_id2, t.del, t.create_by, t.update_by FROM `user` t LEFT JOIN address t2 ON t2.user_id = t.id " +
                "WHERE t.del = false AND (t.id <= ?)");
        List<UserDO> userDO = userMapper.selectJoinList(UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10).lambda());
        System.out.println(userDO);

        ThreadLocalUtils.set("SELECT t.id, t.pid, t.`name`, t.`json`, t.sex, t.head_img, t.create_time, t.address_id, " +
                "t.address_id2, t.del, t.create_by, t.update_by, t2.address AS userAddress FROM `user` t " +
                "LEFT JOIN address t2 ON t2.user_id = t.id WHERE t.del = false AND (t.id <= ?)");
        List<UserDTO> dto = userMapper.selectJoinList(UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10).lambda());
        System.out.println(dto);
    }

    @Test
    void test6() {
        ThreadLocalUtils.set("SELECT t.id AS idea, t.user_id AS uuid, t.tenant_id FROM user_tenant t WHERE (t.id <= ?) AND t.tenant_id = 1");
        List<UserTenantDO> userDO = userTenantMapper.selectJoinList(UserTenantDO.class, new MPJQueryWrapper<UserTenantDO>()
                .selectAll(UserTenantDO.class)
                .le("t.id ", 10).lambda());
        System.out.println(userDO);
    }

    @Test
    void test7() {
        ThreadLocalUtils.set("SELECT tt.id AS idea, tt.user_id AS uuid, tt.tenant_id FROM user_tenant tt WHERE (tt.id <= ?) AND tt.tenant_id = 1");
        MPJQueryWrapper<UserTenantDO> wrapper = new MPJQueryWrapper<UserTenantDO>()
                .setAlias("tt")
                .selectAll(UserTenantDO.class,"tt")
                .le("tt.id ", 10);
        System.out.println(wrapper.getAlias());
        List<UserTenantDO> userDO = userTenantMapper.selectJoinList(UserTenantDO.class, wrapper.lambda());
        System.out.println(wrapper.getAlias());
        System.out.println(userDO);
    }

}
