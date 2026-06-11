package com.dianping.service;

import com.dianping.common.Result;
import com.dianping.vo.UserVO;
import java.util.List;

public interface IFollowService {
    Result follow(Long followUserId, boolean isFollow);
    boolean isFollow(Long followUserId);
    List<UserVO> commonFollow(Long userId);
    List<UserVO> queryFollows(Long userId);
}
