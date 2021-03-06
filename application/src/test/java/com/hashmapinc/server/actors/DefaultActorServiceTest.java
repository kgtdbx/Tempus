/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * Modifications © 2017-2018 Hashmap, Inc
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
package com.hashmapinc.server.actors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import com.google.common.util.concurrent.Futures;
import com.hashmapinc.server.common.data.id.*;
import com.hashmapinc.server.common.data.plugin.ComponentDescriptor;
import com.hashmapinc.server.common.data.security.DeviceTokenCredentials;
import com.hashmapinc.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import com.hashmapinc.server.common.msg.session.BasicToDeviceActorSessionMsg;
import com.hashmapinc.server.common.msg.session.SessionContext;
import com.hashmapinc.server.common.msg.session.SessionType;
import com.hashmapinc.server.dao.attributes.AttributesService;
import com.hashmapinc.server.dao.cluster.NodeMetricService;
import com.hashmapinc.server.gen.discovery.ServerInstanceProtos;
import com.hashmapinc.server.service.cluster.discovery.ServerInstance;
import com.hashmapinc.server.actors.service.DefaultActorService;
import com.hashmapinc.server.common.data.id.*;
import com.hashmapinc.server.common.data.kv.TsKvEntry;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.plugin.ComponentLifecycleState;
import com.hashmapinc.server.common.data.plugin.ComponentType;
import com.hashmapinc.server.common.msg.session.*;
import com.hashmapinc.server.dao.event.EventService;
import com.hashmapinc.server.service.cluster.discovery.DiscoveryService;
import com.hashmapinc.server.service.cluster.routing.ClusterRoutingService;
import com.hashmapinc.server.service.cluster.rpc.ClusterRpcService;
import com.hashmapinc.server.service.component.ComponentDiscoveryService;
import com.hashmapinc.server.common.transport.auth.DeviceAuthResult;
import com.hashmapinc.server.common.transport.auth.DeviceAuthService;
import com.hashmapinc.server.common.data.DataConstants;
import com.hashmapinc.server.common.data.Device;
import com.hashmapinc.server.common.data.Tenant;
import com.hashmapinc.server.common.data.kv.BasicTsKvEntry;
import com.hashmapinc.server.common.data.kv.KvEntry;
import com.hashmapinc.server.common.data.kv.StringDataEntry;
import com.hashmapinc.server.common.data.plugin.PluginMetaData;
import com.hashmapinc.server.common.data.rule.RuleMetaData;
import com.hashmapinc.server.common.data.security.DeviceCredentialsFilter;
import com.hashmapinc.server.common.msg.core.BasicTelemetryUploadRequest;
import com.hashmapinc.server.dao.device.DeviceService;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.plugin.PluginService;
import com.hashmapinc.server.dao.rule.RuleService;
import com.hashmapinc.server.dao.tenant.TenantService;
import com.hashmapinc.server.dao.timeseries.TimeseriesService;
import com.hashmapinc.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultActorServiceTest {

    private static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);

    private static final String PLUGIN_ID = "9fb2e951-e298-4acb-913a-db69af8a15f4";
    private static final String FILTERS_CONFIGURATION =
            "[{\"clazz\":\"com.hashmapinc.server.extensions.core.filter.MsgTypeFilter\", \"name\":\"TelemetryFilter\", \"configuration\": {\"messageTypes\":[\"POST_TELEMETRY\",\"POST_ATTRIBUTES\",\"GET_ATTRIBUTES\"]}}]";
    private static final String ACTION_CONFIGURATION = "{\"pluginToken\":\"telemetry\", \"clazz\":\"com.hashmapinc.server.extensions.core.action.telemetry.TelemetryPluginAction\", \"name\":\"TelemetryMsgConverterAction\", \"configuration\":{}}";
    private static final String PLUGIN_CONFIGURATION = "{}";
    private DefaultActorService actorService;
    private ActorSystemContext actorContext;

    private PluginService pluginService;
    private RuleService ruleService;
    private DeviceAuthService deviceAuthService;
    private DeviceService deviceService;
    private TimeseriesService tsService;
    private TenantService tenantService;
    private ClusterRpcService rpcService;
    private DiscoveryService discoveryService;
    private ClusterRoutingService routingService;
    private AttributesService attributesService;
    private ComponentDiscoveryService componentService;
    private EventService eventService;
    private ServerInstance serverInstance;
    private NodeMetricService nodeMetricService;

    private RuleMetaData ruleMock;
    private PluginMetaData pluginMock;
    private RuleId ruleId = new RuleId(UUID.randomUUID());
    private PluginId pluginId = new PluginId(UUID.fromString(PLUGIN_ID));
    private TenantId tenantId = new TenantId(UUID.randomUUID());


    @Before
    public void before() throws Exception {
        actorService = new DefaultActorService();
        actorContext = new ActorSystemContext();

        tenantService = mock(TenantService.class);
        pluginService = mock(PluginService.class);
        ruleService = mock(RuleService.class);
        deviceAuthService = mock(DeviceAuthService.class);
        deviceService = mock(DeviceService.class);
        tsService = mock(TimeseriesService.class);
        rpcService = mock(ClusterRpcService.class);
        discoveryService = mock(DiscoveryService.class);
        routingService = mock(ClusterRoutingService.class);
        attributesService = mock(AttributesService.class);
        componentService = mock(ComponentDiscoveryService.class);
        eventService = mock(EventService.class);
        nodeMetricService = mock(NodeMetricService.class);
        serverInstance = new ServerInstance(ServerInstanceProtos.ServerInfo.newBuilder().setHost("localhost").setPort(8080).build());

        ReflectionTestUtils.setField(actorService, "actorContext", actorContext);
        ReflectionTestUtils.setField(actorService, "rpcService", rpcService);
        ReflectionTestUtils.setField(actorService, "discoveryService", discoveryService);

        ReflectionTestUtils.setField(actorContext, "syncSessionTimeout", 10000L);
        ReflectionTestUtils.setField(actorContext, "pluginActorTerminationDelay", 10000L);
        ReflectionTestUtils.setField(actorContext, "pluginErrorPersistFrequency", 10000L);
        ReflectionTestUtils.setField(actorContext, "ruleActorTerminationDelay", 10000L);
        ReflectionTestUtils.setField(actorContext, "ruleErrorPersistFrequency", 10000L);
        ReflectionTestUtils.setField(actorContext, "pluginProcessingTimeout", 60000L);
        ReflectionTestUtils.setField(actorContext, "tenantService", tenantService);
        ReflectionTestUtils.setField(actorContext, "pluginService", pluginService);
        ReflectionTestUtils.setField(actorContext, "ruleService", ruleService);
        ReflectionTestUtils.setField(actorContext, "deviceAuthService", deviceAuthService);
        ReflectionTestUtils.setField(actorContext, "deviceService", deviceService);
        ReflectionTestUtils.setField(actorContext, "tsService", tsService);
        ReflectionTestUtils.setField(actorContext, "rpcService", rpcService);
        ReflectionTestUtils.setField(actorContext, "discoveryService", discoveryService);
        ReflectionTestUtils.setField(actorContext, "tsService", tsService);
        ReflectionTestUtils.setField(actorContext, "routingService", routingService);
        ReflectionTestUtils.setField(actorContext, "attributesService", attributesService);
        ReflectionTestUtils.setField(actorContext, "componentService", componentService);
        ReflectionTestUtils.setField(actorContext, "eventService", eventService);
        ReflectionTestUtils.setField(actorContext, "nodeMetricService", nodeMetricService);


        when(routingService.resolveById((EntityId) any())).thenReturn(Optional.empty());

        when(discoveryService.getCurrentServer()).thenReturn(serverInstance);

        ruleMock = mock(RuleMetaData.class);
        when(ruleMock.getId()).thenReturn(ruleId);
        when(ruleMock.getState()).thenReturn(ComponentLifecycleState.ACTIVE);
        when(ruleMock.getPluginToken()).thenReturn("telemetry");
        TextPageData<RuleMetaData> systemRules = new TextPageData<>(Collections.emptyList(), null, false);
        TextPageData<RuleMetaData> tenantRules = new TextPageData<>(Collections.singletonList(ruleMock), null, false);
        when(ruleService.findSystemRules(any())).thenReturn(systemRules);
        when(ruleService.findTenantRules(any(), any())).thenReturn(tenantRules);
        when(ruleService.findRuleById(ruleId)).thenReturn(ruleMock);

        pluginMock = mock(PluginMetaData.class);
        when(pluginMock.getTenantId()).thenReturn(SYSTEM_TENANT);
        when(pluginMock.getId()).thenReturn(pluginId);
        when(pluginMock.getState()).thenReturn(ComponentLifecycleState.ACTIVE);
        TextPageData<PluginMetaData> systemPlugins = new TextPageData<>(Collections.singletonList(pluginMock), null, false);
        TextPageData<PluginMetaData> tenantPlugins = new TextPageData<>(Collections.emptyList(), null, false);
        when(pluginService.findSystemPlugins(any())).thenReturn(systemPlugins);
        when(pluginService.findTenantPlugins(any(), any())).thenReturn(tenantPlugins);
        when(pluginService.findPluginByApiToken("telemetry")).thenReturn(pluginMock);
        when(pluginService.findPluginById(pluginId)).thenReturn(pluginMock);

        TextPageData<Tenant> tenants = new TextPageData<>(Collections.emptyList(), null, false);
        when(tenantService.findTenants(any())).thenReturn(tenants);
    }

    private void initActorSystem() {
        actorService.initActorSystem();
    }

    @After
    public void after() {
        actorService.stopActorSystem();
    }

    @Test
    public void testBasicPostWithSyncSession() throws Exception {
        SessionContext ssnCtx = mock(SessionContext.class);
        KvEntry entry1 = new StringDataEntry("key1", "value1");
        KvEntry entry2 = new StringDataEntry("key2", "value2");
        BasicTelemetryUploadRequest telemetry = new BasicTelemetryUploadRequest();
        long ts = 42;
        telemetry.add(ts, entry1);
        telemetry.add(ts, entry2);
        BasicAdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(ssnCtx, telemetry);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        DeviceCredentialsFilter filter = new DeviceTokenCredentials("token1");
        Device device = mock(Device.class);

        when(device.getId()).thenReturn(deviceId);
        when(device.getTenantId()).thenReturn(tenantId);
        when(ssnCtx.getSessionId()).thenReturn(new DummySessionID("session1"));
        when(ssnCtx.getSessionType()).thenReturn(SessionType.SYNC);
        when(deviceAuthService.process(filter)).thenReturn(DeviceAuthResult.of(deviceId));
        when(deviceService.findDeviceById(deviceId)).thenReturn(device);

        ObjectMapper ruleMapper = new ObjectMapper();
        when(ruleMock.getFilters()).thenReturn(ruleMapper.readTree(FILTERS_CONFIGURATION));
        when(ruleMock.getAction()).thenReturn(ruleMapper.readTree(ACTION_CONFIGURATION));

        ComponentDescriptor filterComp = new ComponentDescriptor();
        filterComp.setClazz("com.hashmapinc.server.extensions.core.filter.MsgTypeFilter");
        filterComp.setType(ComponentType.FILTER);
        when(componentService.getComponent("com.hashmapinc.server.extensions.core.filter.MsgTypeFilter"))
                .thenReturn(Optional.of(filterComp));

        ComponentDescriptor actionComp = new ComponentDescriptor();
        actionComp.setClazz("com.hashmapinc.server.extensions.core.action.telemetry.TelemetryPluginAction");
        actionComp.setType(ComponentType.ACTION);
        when(componentService.getComponent("com.hashmapinc.server.extensions.core.action.telemetry.TelemetryPluginAction"))
                .thenReturn(Optional.of(actionComp));

        ObjectMapper pluginMapper = new ObjectMapper();
        JsonNode pluginAdditionalInfo = pluginMapper.readTree(PLUGIN_CONFIGURATION);
        when(pluginMock.getConfiguration()).thenReturn(pluginAdditionalInfo);
        when(pluginMock.getClazz()).thenReturn(TelemetryStoragePlugin.class.getName());

        when(attributesService.findAll(deviceId, DataConstants.CLIENT_SCOPE)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        when(attributesService.findAll(deviceId, DataConstants.SHARED_SCOPE)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        when(attributesService.findAll(deviceId, DataConstants.SERVER_SCOPE)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        when(tsService.findAllLatest(deviceId)).thenReturn(Futures.immediateFuture(Collections.emptyList()));

        initActorSystem();
        Thread.sleep(1000); //NOSONAR Added for test
        actorService.process(new BasicToDeviceActorSessionMsg(device, msg));

        // Check that device data was saved to DB;
        List<TsKvEntry> expected = new ArrayList<>();
        expected.add(new BasicTsKvEntry(ts, entry1));
        expected.add(new BasicTsKvEntry(ts, entry2));
        verify(tsService, Mockito.timeout(5000)).save(deviceId, expected, 0L);

    }

}
