package com.cn.bdth.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cn.bdth.entity.Drawing;
import com.cn.bdth.entity.User;
import com.cn.bdth.mapper.DrawingMapper;
import com.cn.bdth.mapper.UserMapper;
import com.cn.bdth.utils.AliUploadUtils;
import com.cn.bdth.utils.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 雨纷纷旧故里草木深
 *
 * @author 时间海 @github dulaiduwang003
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RewardTaskTimer {

    private final UserMapper userMapper;

    private final DrawingMapper drawingMapper;

    private final AliUploadUtils aliUploadUtils;

    @Scheduled(cron = " 0 0 0 * * ?")
//    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void executeTask() {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<User>()
                .setSql("is_sign_in = 0,update_time = update_time");
        userMapper.update(null, updateWrapper);

        //清除无效绘图
        drawingMapper.getCleanDrawing().forEach(this::deleteResource);

        drawingMapper.selectList(new QueryWrapper<Drawing>()
                .lambda()
                .eq(Drawing::getEnv, 0)
                .lt(Drawing::getCreatedTime, LocalDateTime.now().minusDays(2))
        ).forEach(this::deleteResource);

    }

    private void deleteResource(Drawing drawing) {
        final String generateUrl = drawing.getGenerateUrl();
        if (!StringUtils.notEmpty(generateUrl)) {
            final String originalUrl = drawing.getOriginalUrl();
            try {
                if (StringUtils.notEmpty(originalUrl)) {
                    aliUploadUtils.deleteFile(originalUrl);
                }
                if (StringUtils.notEmpty(generateUrl)) {
                    aliUploadUtils.deleteFile(generateUrl);
                }
                //删除图片数据--彻底删除
                drawingMapper.cleanDrawingById(drawing.getDrawingId());
            } catch (Exception e) {
                log.warn("删除OSS图片数据失败");
            }
        }
    }
}
