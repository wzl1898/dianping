package com.dianping.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeckillVoucherDTO {
    private Long voucherId;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}
