package com.xz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_gateway")
public class GateWayInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String requestPath;
    private String redirectUrl;
    private String routeName;
}
