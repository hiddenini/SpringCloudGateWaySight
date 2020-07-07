package com.xz;


import com.Application;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * @author xz
 * @date 2020/4/22 15:33
 **/

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class SampleTest {

    @Autowired
    private GateWayMapper gateWayMapper;

    @Test
    public void testDelete() {
        QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", "jd_route");
        gateWayMapper.delete(queryWrapper);
    }
}
