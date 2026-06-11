package com.dianping.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BlogVO {
    private Long id;
    private Long shopId;
    private Long userId;
    private String title;
    private String images;
    private String content;
    private Integer liked;
    private Integer comments;
    private LocalDateTime createTime;
    private UserVO author;
    private Boolean isLiked;
}
