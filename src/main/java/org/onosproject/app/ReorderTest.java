/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.app;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An ONOS application that reproduces FlowRuleEvent reordering.
 */
@Component(immediate = true)
public class ReorderTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final DeviceId target = DeviceId.deviceId("of:0000000000000001");
    private final PortNumber port1 = PortNumber.portNumber(1);
    private final PortNumber port2 = PortNumber.portNumber(2);

    private ApplicationId appId;
    private FlowRuleEvent lastEvent;

    private final int iterations = 100;

    @Activate
    protected void activate() {
        log.info("Started");
        appId = coreService.getAppId("org.onosproject.reordertest");
        flowRuleService.addListener(this::listen);
        threadPool.execute(this::issue);
    }

    private void issue() {
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(target)
                .withSelector(DefaultTrafficSelector.builder().matchInPort(port1).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(port2).build())
                .fromApp(appId)
                .withPriority(0)
                .makePermanent()
                .build();
        FlowRuleOperations add = FlowRuleOperations.builder().add(rule).build();
        FlowRuleOperations remove = FlowRuleOperations.builder().remove(rule).build();

        for (int i = 0; i < iterations; i++) {
            FlowRuleOperations ops;
            if (i % 2 == 0) {
                ops = add;
            } else {
                ops = remove;
            }
            log.info("Applying FlowRuleOperations: {}", ops);
            flowRuleService.apply(ops);
        }
    }

    private void listen(FlowRuleEvent event) {
        if (event.type() != FlowRuleEvent.Type.RULE_ADD_REQUESTED &&
                event.type() != FlowRuleEvent.Type.RULE_REMOVE_REQUESTED) {
            return;
        }

        log.info("Event received: {}", event);
        synchronized (this) {
            if (lastEvent == null) {
                lastEvent = event;
            } else {
                if (event.type() == lastEvent.type()) {
                    log.warn("Reorder happens: last={}, current={}", lastEvent, event);
                }
                lastEvent = event;
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

}
