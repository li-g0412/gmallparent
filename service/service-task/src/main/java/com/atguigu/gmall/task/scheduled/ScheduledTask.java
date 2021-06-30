package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //  编写一个定时任务 : 分时日月周年
    //  @Scheduled(cron = "0 0 1 * * ?") 凌晨一点中！
    @Scheduled(cron = "0/10 * * * * ?") // 每隔30秒触发一次！
    public void testTask(){
        //  System.out.println("来人了，开始接客吧.....");
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"预热吧....");
    }
}
