/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi;

import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.drivers.microsemi.yang.MockMseaUniEvcServiceManager;
import org.onosproject.drivers.microsemi.yang.MockNetconfSessionEa1000;
import org.onosproject.drivers.netconf.MockNetconfController;
import org.onosproject.drivers.netconf.MockNetconfDevice;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.Meter.Unit;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperation.Type;
import org.onosproject.netconf.NetconfController;

public class EA1000MeterProviderTest {

    private EA1000MeterProvider meterProvider;
    private NetconfController controller;
    private DeviceId mockDeviceId;
    private MockMseaUniEvcServiceManager mseaUniEvcServiceSvc;

    @Before
    public void setUp() throws Exception {
        mockDeviceId = DeviceId.deviceId("netconf:1.2.3.4:830");
        controller = new MockNetconfController();
        MockNetconfDevice device = (MockNetconfDevice) controller.connectDevice(mockDeviceId);
        device.setNcSessionImpl(MockNetconfSessionEa1000.class);
        mseaUniEvcServiceSvc = new MockMseaUniEvcServiceManager();
        mseaUniEvcServiceSvc.activate();
        meterProvider = new TestEA1000MeterProvider(controller, mseaUniEvcServiceSvc);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPerformMeterOperationDeviceIdMeterAdd() {
        DeviceId mockDeviceId = DeviceId.deviceId("netconf:1.2.3.4:830");

        Band cbsBand = DefaultBand.builder()
                .ofType(Band.Type.REMARK) //Committed - CIR & CBS
                .withRate(37500L)
                .burstSize(2000)
                .dropPrecedence((short) 0)
                .build();

        Band ebsBand = DefaultBand.builder()
                .ofType(Band.Type.DROP) //Excess - EIR & EBS
                .withRate(50000L) //The rate at which we drop - for EA 1000 subtract CIR to get EIR
                .burstSize(3000) //The burst rate to drop at
                .build();

        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(mockDeviceId)
                .withId(MeterId.meterId(1))
                .fromApp(new DefaultApplicationId(101, "unit.test"))
                .burst()
                .withUnit(Unit.KB_PER_SEC)
                .withBands(new HashSet<Band>() { { add(cbsBand); add(ebsBand); } });

        MeterOperation meterOp = new MeterOperation(mBuilder.build(), Type.ADD);

        meterProvider.performMeterOperation(mockDeviceId, meterOp);
        //The NETCONF XML generated by this matches the pattern
        // sampleXmlRegexEditConfigBwpGroup1
        // in MockNetconfSession
    }

    @Test
    @Ignore("fixme: failing with onos-yang-tools-2.2.0-b1")
    public void testPerformMeterOperationDeviceIdMeterRemove() {
        DeviceId mockDeviceId = DeviceId.deviceId("netconf:1.2.3.4:830");

        Band cbsBand = DefaultBand.builder()
                .ofType(Band.Type.REMARK) //Committed - CIR & CBS
                .withRate(37500L)
                .burstSize(2000)
                .dropPrecedence((short) 0)
                .build();

        Meter.Builder mBuilder = DefaultMeter.builder()
                .forDevice(mockDeviceId)
                .withId(MeterId.meterId(1))
                .fromApp(new DefaultApplicationId(101, "unit.test"))
                .burst()
                .withBands(new HashSet<Band>() { { add(cbsBand); } });

        MeterOperation meterOp = new MeterOperation(mBuilder.build(), Type.REMOVE);

        meterProvider.performMeterOperation(mockDeviceId, meterOp);
        //The NETCONF XML generated by this matches the pattern
        // sampleXmlRegexEditConfigBwpGroup1
        // in MockNetconfSession
    }

}
