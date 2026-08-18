package com.share.rules.test;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ServiceRulesApplicationTest {
    @Autowired
    private KieContainer kieContainer;

    @Test
    void test1() {
        // 开启会话
        KieSession kieSession = kieContainer.newKieSession();

        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
    }
}
