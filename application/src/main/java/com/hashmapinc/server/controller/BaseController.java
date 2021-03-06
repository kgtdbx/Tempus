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
package com.hashmapinc.server.controller;

import com.hashmapinc.server.actors.service.ActorService;
import com.hashmapinc.server.common.data.*;
import com.hashmapinc.server.common.data.alarm.Alarm;
import com.hashmapinc.server.common.data.alarm.AlarmId;
import com.hashmapinc.server.common.data.alarm.AlarmInfo;
import com.hashmapinc.server.common.data.asset.Asset;
import com.hashmapinc.server.common.data.audit.ActionType;
import com.hashmapinc.server.common.data.computation.ComputationJob;
import com.hashmapinc.server.common.data.datamodel.DataModelObject;
import com.hashmapinc.server.common.data.id.*;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.common.data.page.TimePageLink;
import com.hashmapinc.server.common.data.plugin.ComponentDescriptor;
import com.hashmapinc.server.common.data.plugin.ComponentType;
import com.hashmapinc.server.common.data.plugin.PluginMetaData;
import com.hashmapinc.server.common.data.rule.RuleMetaData;
import com.hashmapinc.server.common.data.security.Authority;
import com.hashmapinc.server.common.data.widget.WidgetType;
import com.hashmapinc.server.common.data.widget.WidgetsBundle;
import com.hashmapinc.server.dao.alarm.AlarmService;
import com.hashmapinc.server.dao.asset.AssetService;
import com.hashmapinc.server.dao.audit.AuditLogService;
import com.hashmapinc.server.dao.cluster.NodeMetricService;
import com.hashmapinc.server.dao.computations.ComputationJobService;
import com.hashmapinc.server.dao.computations.ComputationsService;
import com.hashmapinc.server.dao.customer.CustomerService;
import com.hashmapinc.server.dao.customergroup.CustomerGroupService;
import com.hashmapinc.server.dao.dashboard.DashboardService;
import com.hashmapinc.server.dao.datamodel.DataModelObjectService;
import com.hashmapinc.server.dao.datamodel.DataModelService;
import com.hashmapinc.server.dao.device.DeviceCredentialsService;
import com.hashmapinc.server.dao.device.DeviceService;
import com.hashmapinc.server.dao.exception.DataValidationException;
import com.hashmapinc.server.dao.exception.IncorrectParameterException;
import com.hashmapinc.server.dao.gatewayconfiguration.TempusGatewayConfigurationService;
import com.hashmapinc.server.dao.metadataingestion.MetadataIngestionService;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.plugin.PluginDataEncoderService;
import com.hashmapinc.server.dao.plugin.PluginService;
import com.hashmapinc.server.dao.relation.RelationService;
import com.hashmapinc.server.dao.rule.RuleService;
import com.hashmapinc.server.dao.user.UserService;
import com.hashmapinc.server.dao.widget.WidgetTypeService;
import com.hashmapinc.server.dao.widget.WidgetsBundleService;
import com.hashmapinc.server.common.data.exception.TempusErrorCode;
import com.hashmapinc.server.exception.TempusErrorResponseHandler;
import com.hashmapinc.server.common.data.exception.TempusException;
import com.hashmapinc.server.service.component.ComponentDiscoveryService;
import com.hashmapinc.server.service.kubernetes.gateway.TempusGatewayKubernetesService;
import com.hashmapinc.server.service.metadataingestion.MetadataConfigService;
import com.hashmapinc.server.service.metadataingestion.MetadataQueryService;
import com.hashmapinc.server.service.security.auth.AttributeBasedPermissionEvaluator;
import com.hashmapinc.server.service.security.model.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hashmapinc.server.dao.service.Validator.validateId;

@Slf4j
public abstract class BaseController {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    private static final String ITEM_NOT_FOUND = "Requested item wasn't found!";
    public static final String NO_PERMISSION_TO_READ = "You don't have permission to read!";

    @Autowired
    private TempusErrorResponseHandler errorResponseHandler;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected DataModelService dataModelService;

    @Autowired
    protected DataModelObjectService dataModelObjectService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleService ruleService;

    @Autowired
    protected PluginService pluginService;

    @Autowired
    protected ActorService actorService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected ComputationJobService computationJobService;

    @Autowired
    protected ComputationsService computationsService;


    @Autowired
    protected AuditLogService auditLogService;

    @Autowired
    protected NodeMetricService nodeMetricService;

    @Autowired
    protected MetadataIngestionService metadataIngestionService;

    @Autowired
    protected MetadataConfigService metadataConfigService;

    @Autowired
    protected MetadataQueryService metadataQueryService;

    @Autowired
    protected CustomerGroupService customerGroupService;

    @Autowired
    protected TempusGatewayConfigurationService tempusGatewayConfigurationService;

    @Autowired
    protected TempusGatewayKubernetesService tempusGatewayKubernetesService;

    @Autowired
    protected AttributeBasedPermissionEvaluator evaluator;

    @Autowired
    protected PluginDataEncoderService pluginDataEncoderService;

    @ExceptionHandler(TempusException.class)
    public void handleTempusException(TempusException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    TempusException handleException(Exception exception) {
        return handleException(exception, true);
    }

    private TempusException handleException(Exception exception, boolean logException) {
        if (logException) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof TempusException) {
            return (TempusException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new TempusException(exception.getMessage(), TempusErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new TempusException("Unable to send mail: " + exception.getMessage(), TempusErrorCode.GENERAL);
        } else if (exception instanceof HttpClientErrorException && ((HttpClientErrorException) exception).getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new TempusException(ITEM_NOT_FOUND, TempusErrorCode.ITEM_NOT_FOUND);
        } else if (exception instanceof HttpClientErrorException && ((HttpClientErrorException) exception).getStatusCode().equals(HttpStatus.CONFLICT)) {
            return new TempusException(((HttpClientErrorException) exception).getResponseBodyAsString(), TempusErrorCode.CONFLICT_ITEM);
        } else {
            return new TempusException(exception.getMessage(), TempusErrorCode.GENERAL);
        }
    }

    <T> T checkNotNull(T reference) throws TempusException {
        if (reference == null) {
            throw new TempusException(ITEM_NOT_FOUND, TempusErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws TempusException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new TempusException(ITEM_NOT_FOUND, TempusErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws TempusException {
        if (StringUtils.isEmpty(param)) {
            throw new TempusException("Parameter '" + name + "' can't be empty!", TempusErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, String[] params) throws TempusException {
        if (params == null || params.length == 0) {
            throw new TempusException("Parameter '" + name + "' can't be empty!", TempusErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    UUID toUUID(String id) {
        return UUID.fromString(id);
    }

    TimePageLink createPageLink(int limit, Long startTime, Long endTime, boolean ascOrder, String idOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TimePageLink(limit, startTime, endTime, ascOrder, idOffsetUuid);
    }


    TextPageLink createPageLink(int limit, String textSearch, String idOffset, String textOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TextPageLink(limit, textSearch, idOffsetUuid, textOffset);
    }

    protected SecurityUser getCurrentUser() throws TempusException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new TempusException("You aren't authorized to perform this operation!", TempusErrorCode.AUTHENTICATION);
        }
    }

    void checkTenantId(TenantId tenantId) throws TempusException {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() != Authority.SYS_ADMIN &&
                (authUser.getTenantId() == null || !authUser.getTenantId().equals(tenantId))) {
            throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    TempusErrorCode.PERMISSION_DENIED);
        }
    }

    protected TenantId getTenantId() throws TempusException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(CustomerId customerId) throws TempusException {
        try {
            validateId(customerId, "Incorrect customerId " + customerId);
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.SYS_ADMIN ||
                    (authUser.getAuthority() != Authority.TENANT_ADMIN &&
                            (authUser.getCustomerId() == null || !authUser.getCustomerId().equals(customerId)))) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            Customer customer = customerService.findCustomerById(customerId);
            checkCustomer(customer);
            return customer;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    CustomerGroup checkCustomerGroupId(CustomerGroupId customerGroupId) throws TempusException {
        try {
            validateId(customerGroupId, "Incorrect customerGroupId " + customerGroupId);
            SecurityUser authUser = getCurrentUser();
            CustomerGroup customerGroup = customerGroupService.findByCustomerGroupId(customerGroupId);
            checkCustomerGroup(customerGroup);
            if (!customerGroup.getTenantId().getId().equals(authUser.getTenantId().getId())) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            return customerGroup;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TempusGatewayConfiguration checkTempusGatewayConfigurationId(TempusGatewayConfigurationId tempusGatewayConfigurationId) throws TempusException {
        try {
            validateId(tempusGatewayConfigurationId, "Incorrect tempusGatewayConfigurationId " + tempusGatewayConfigurationId);
            SecurityUser authUser = getCurrentUser();
            TempusGatewayConfiguration tempusGatewayConfiguration = tempusGatewayConfigurationService.findTempusGatewayConfigurationById(tempusGatewayConfigurationId);
            checkTempusGatewayConfiguration(tempusGatewayConfiguration);
            if (!tempusGatewayConfiguration.getTenantId().getId().equals(authUser.getTenantId().getId())) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
            }
            return tempusGatewayConfiguration;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Long checkLong(String value, String paramName) {
        try {
           return Long.parseLong(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incorrect "+paramName + " value supplied");
        }
    }

    Double checkDouble(String value, String paramName) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incorrect "+paramName + " value supplied");
        }
    }


    private void checkCustomer(Customer customer) throws TempusException {
        checkNotNull(customer);
        checkTenantId(customer.getTenantId());
    }

    private void checkCustomerGroup(CustomerGroup customerGroup) throws TempusException {
        checkNotNull(customerGroup);
        checkTenantId(customerGroup.getTenantId());
        checkCustomerId(customerGroup.getCustomerId());
    }

    private void checkTempusGatewayConfiguration(TempusGatewayConfiguration tempusGatewayConfiguration) throws TempusException {
        checkNotNull(tempusGatewayConfiguration);
        checkTenantId(tempusGatewayConfiguration.getTenantId());
    }

    User checkUserId(UserId userId) throws TempusException {
        try {
            validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(userId);
            checkUser(user);
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkUser(User user) throws TempusException {
        checkNotNull(user);
        checkTenantId(user.getTenantId());
        if (user.getAuthority() == Authority.CUSTOMER_USER) {
            checkCustomerId(user.getCustomerId());
        }
    }

    protected void checkEntityId(EntityId entityId) throws TempusException {
        try {
            checkNotNull(entityId);
            validateId(entityId.getId(), "Incorrect entityId " + entityId);
            switch (entityId.getEntityType()) {
                case DEVICE:
                    checkDevice(deviceService.findDeviceById(new DeviceId(entityId.getId())));
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()));
                    return;
                case TENANT:
                    checkTenantId(new TenantId(entityId.getId()));
                    return;
                case PLUGIN:
                    checkPlugin(new PluginId(entityId.getId()));
                    return;
                case RULE:
                    checkRule(new RuleId(entityId.getId()));
                    return;
                case ASSET:
                    checkAsset(assetService.findAssetById(new AssetId(entityId.getId())));
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()));
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()));
                    return;
                case CUSTOMER_GROUP:
                    checkCustomerGroupId(new CustomerGroupId(entityId.getId()));
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(DeviceId deviceId) throws TempusException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(deviceId);
            checkDevice(device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkDevice(Device device) throws TempusException {
        checkNotNull(device);
        checkTenantId(device.getTenantId());
        if (device.getCustomerId() != null && !device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(device.getCustomerId());
        }
    }

    Asset checkAssetId(AssetId assetId) throws TempusException {
        try {
            validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(assetId);
            checkAsset(asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAsset(Asset asset) throws TempusException {
        checkNotNull(asset);
        checkTenantId(asset.getTenantId());
        if (asset.getCustomerId() != null && !asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(asset.getCustomerId());
        }
    }

    DataModelObject checkDataModelObjectId(DataModelObjectId dataModelObjectId) throws TempusException {
        try {
            validateId(dataModelObjectId, "Incorrect dataModelObjcetId " + dataModelObjectId);
            DataModelObject dataModelObject = dataModelObjectService.findById(dataModelObjectId);
            checkDataModelObject(dataModelObject);
            return dataModelObject;
        } catch (Exception e) {
            throw  handleException(e, false);
        }
    }

    private void checkDataModelObject(DataModelObject dataModelObject) throws TempusException {
        checkNotNull(dataModelObject);
        if (dataModelObject.getCustomerId() != null && !dataModelObject.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(dataModelObject.getCustomerId());
        }
    }

    Alarm checkAlarmId(AlarmId alarmId) throws TempusException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            Alarm alarm = alarmService.findAlarmByIdAsync(alarmId).get();
            checkAlarm(alarm);
            return alarm;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId) throws TempusException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            AlarmInfo alarmInfo = alarmService.findAlarmInfoByIdAsync(alarmId).get();
            checkAlarm(alarmInfo);
            return alarmInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAlarm(Alarm alarm) throws TempusException {
        checkNotNull(alarm);
        checkTenantId(alarm.getTenantId());
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, boolean modify) throws TempusException {
        try {
            validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(widgetsBundleId);
            checkWidgetsBundle(widgetsBundle, modify);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkWidgetsBundle(WidgetsBundle widgetsBundle, boolean modify) throws TempusException {
        checkNotNull(widgetsBundle);
        if (widgetsBundle.getTenantId() != null && !widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetsBundle.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    TempusErrorCode.PERMISSION_DENIED);
        }
    }

    WidgetType checkWidgetTypeId(WidgetTypeId widgetTypeId, boolean modify) throws TempusException {
        try {
            validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetType widgetType = widgetTypeService.findWidgetTypeById(widgetTypeId);
            checkWidgetType(widgetType, modify);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    void checkWidgetType(WidgetType widgetType, boolean modify) throws TempusException {
        checkNotNull(widgetType);
        if (widgetType.getTenantId() != null && !widgetType.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetType.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    TempusErrorCode.PERMISSION_DENIED);
        }
    }

    Dashboard checkDashboardId(DashboardId dashboardId) throws TempusException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(dashboardId);
            checkDashboard(dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId) throws TempusException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            DashboardInfo dashboardInfo = dashboardService.findDashboardInfoById(dashboardId);
            checkDashboard(dashboardInfo);
            return dashboardInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkDashboard(DashboardInfo dashboard) throws TempusException {
        checkNotNull(dashboard);
        checkTenantId(dashboard.getTenantId());
        SecurityUser authUser = getCurrentUser();
        final boolean isDashboardNotAssignedToCurrentCustomer = authUser.getAuthority() == Authority.CUSTOMER_USER
                && !dashboard.isAssignedToCustomer(authUser.getCustomerId());

        if (isDashboardNotAssignedToCurrentCustomer && (dashboard.getType() != DashboardType.ASSET_LANDING_PAGE)) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);
        }
    }

    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws TempusException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type) throws TempusException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkPluginActionsByPluginClazz(String pluginClazz) throws TempusException {
        try {
            checkComponentDescriptorByClazz(pluginClazz);
            log.debug("[{}] Lookup plugin actions", pluginClazz);
            return componentDescriptorService.getPluginActions(pluginClazz);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected PluginMetaData checkPlugin(PluginMetaData plugin) throws TempusException {
        checkNotNull(plugin);
        SecurityUser authUser = getCurrentUser();
        TenantId tenantId = plugin.getTenantId();
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        if (authUser.getAuthority() != Authority.SYS_ADMIN) {
            if (authUser.getTenantId() == null ||
                    !tenantId.getId().equals(ModelConstants.NULL_UUID) && !authUser.getTenantId().equals(tenantId)) {
                throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        TempusErrorCode.PERMISSION_DENIED);

            } else if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                plugin.setConfiguration(null);
            }
        }
        return plugin;
    }

    protected ComputationJob checkComputationJob(ComputationJob computationJob) throws TempusException {
        checkNotNull(computationJob);
        TenantId tenantId = computationJob.getTenantId();
        validatePermissions(tenantId);
        return computationJob;
    }

    protected PluginMetaData checkPlugin(PluginId pluginId) throws TempusException {
        checkNotNull(pluginId);
        return checkPlugin(pluginService.findPluginById(pluginId));
    }

    protected RuleMetaData checkRule(RuleId ruleId) throws TempusException {
        checkNotNull(ruleId);
        return checkRule(ruleService.findRuleById(ruleId));
    }

    protected RuleMetaData checkRule(RuleMetaData rule) throws TempusException {
        checkNotNull(rule);
        TenantId tenantId = rule.getTenantId();
        validatePermissions(tenantId);
        return rule;
    }

    private void validatePermissions(TenantId tenantId) throws TempusException {
        SecurityUser authUser = getCurrentUser();
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        boolean computationNotBelongsToTenant = authUser.getTenantId() == null ||
                !tenantId.getId().equals(ModelConstants.NULL_UUID) && !authUser.getTenantId().equals(tenantId);
        if (authUser.getAuthority() != Authority.SYS_ADMIN && computationNotBelongsToTenant) {
            throw new TempusException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    TempusErrorCode.PERMISSION_DENIED);

        }
    }

    protected String constructBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        if (request.getHeader("x-forwarded-proto") != null) {
            scheme = request.getHeader("x-forwarded-proto");
        }
        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
                log.trace(e.getMessage());
            }
        }

        return String.format("%s://%s:%d",
                scheme,
                request.getServerName(),
                serverPort);
    }

    protected <I extends UUIDBased & EntityId> I emptyId(EntityType entityType) {
        return (I)EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected <E extends BaseData<I> & HasName,
            I extends UUIDBased & EntityId> void logEntityAction(I entityId, E entity, CustomerId customerId,
                                                                 ActionType actionType, Exception e, Object... additionalInfo) throws TempusException {
        User user = getCurrentUser();
        if (customerId == null || customerId.isNullUid()) {
            customerId = user.getCustomerId();
        }
        auditLogService.logEntityAction(user.getTenantId(), customerId, user.getId(), user.getName(), entityId, entity, actionType, e, additionalInfo);
    }

    protected TempusResourceCriteriaSpec getTempusResourceCriteriaSpec(SecurityUser user, EntityType entityType, DataModelObjectId dataModelObjectId, CustomerId customerId, String type, String searchText) throws TempusException{
        final TempusResourceCriteriaSpec tempusResourceCriteriaSpec = new TempusResourceCriteriaSpec(entityType, user.getTenantId());

        if(type != null && !type.trim().isEmpty()) {
            tempusResourceCriteriaSpec.setType(Optional.of(type.trim()));
        }

        if(searchText != null && !searchText.trim().isEmpty()) {
            tempusResourceCriteriaSpec.setSearchText(Optional.of(searchText));
        }

        final Supplier<Stream<UserPermission>> readableAndResourceAccessibleStream = getReadableAndResourceAccessibleStream(user, entityType);

        final long count = readableAndResourceAccessibleStream.get().map(UserPermission::getResourceAttributes).count();

        if(count == 0){ //case: {USER}::READ
            throw new IncorrectParameterException(NO_PERMISSION_TO_READ);
        }

        final boolean hasPermOnAllResources =
                readableAndResourceAccessibleStream.get().map(UserPermission::getResourceAttributes)
                        .filter(map -> !Objects.isNull(map))
                        .anyMatch(Map::isEmpty); //case: {USER}:*:READ

        Map<DataModelObjectId, Set<? extends EntityId>> dataModelIdAndEntityIdSpec = new HashMap<>();

        if(dataModelObjectId != null) {
            if(!hasPermOnAllResources){
                final boolean hasHierarchicalPermOnEntireDmo =
                        hasHierarchicalPermOnEntireDataModel(readableAndResourceAccessibleStream, dataModelObjectId);  // case: {USER}:ASSET?dataModelId={dmoid}:READ where dmoid in (parent of given dmoid)

                if (!hasHierarchicalPermOnEntireDmo) {
                    final boolean hasPermOnDmoWithResources =
                            hasPermOnDmoWithResources(readableAndResourceAccessibleStream, dataModelObjectId); // case: {USER}:ASSET?dataModelId={dmoid}&id={resId}:READ where dmoid = dataModelObjectId
                    if (!hasPermOnDmoWithResources) {
                        throw new IncorrectParameterException(NO_PERMISSION_TO_READ);
                    } else {
                        final Set<EntityId> accessibleResourceIdsForGivenDmo = getAccessibleResourceIdsForGivenDmo(readableAndResourceAccessibleStream, entityType, dataModelObjectId);
                        accessibleResourceIdsForGivenDmo.addAll(dataModelIdAndEntityIdSpec.getOrDefault(dataModelObjectId, new HashSet<>()));
                        dataModelIdAndEntityIdSpec.put(dataModelObjectId, accessibleResourceIdsForGivenDmo);
                    }
                } else {
                    dataModelIdAndEntityIdSpec.put(dataModelObjectId, Collections.emptySet());
                }
            } else {
                dataModelIdAndEntityIdSpec.put(dataModelObjectId, Collections.emptySet());
            }
        }

        if(customerId != null) {
            if(!hasPermOnAllResources){
                String entityTypeName = entityType.equals(EntityType.DEVICE) ? "Device" : "Asset";
                final DataModelId dataModelId = customerService.findCustomerById(customerId).getDataModelId();
                final List<DataModelObjectId> dataModelObjectIds = (dataModelId != null) ?
                        dataModelObjectService.findByDataModelIdAndType(dataModelId, entityTypeName).stream().map(DataModelObject::getId).collect(Collectors.toList()) :
                        Collections.emptyList();
                if(!dataModelObjectIds.isEmpty()){
                    for (DataModelObjectId _dataModelObjectId : dataModelObjectIds) {
                        final boolean hasHierarchicalPermOnEntireDmo =
                                hasHierarchicalPermOnEntireDataModel(readableAndResourceAccessibleStream, _dataModelObjectId);  // case: {USER}:ASSET?dataModelId={dmoid}:READ where dmoid in (parent of given dmoid)

                        if (!hasHierarchicalPermOnEntireDmo) {
                            final boolean hasPermOnDmoWithResources =
                                    hasPermOnDmoWithResources(readableAndResourceAccessibleStream, _dataModelObjectId); // case: {USER}:ASSET?dataModelId={dmoid}&id={resId}:READ where dmoid = dataModelObjectId
                            if (hasPermOnDmoWithResources) {
                                final Set<EntityId> accessibleResourceIdsForGivenDmo = getAccessibleResourceIdsForGivenDmo(readableAndResourceAccessibleStream, entityType, _dataModelObjectId);
                                accessibleResourceIdsForGivenDmo.addAll(dataModelIdAndEntityIdSpec.getOrDefault(_dataModelObjectId, new HashSet<>()));
                                dataModelIdAndEntityIdSpec.put(_dataModelObjectId, accessibleResourceIdsForGivenDmo);
                            }
                        } else {
                            dataModelIdAndEntityIdSpec.put(_dataModelObjectId, Collections.emptySet());
                        }
                    }
                }
            }
            tempusResourceCriteriaSpec.setCustomerId(Optional.of(customerId));
        }
        tempusResourceCriteriaSpec.setDataModelIdAndEntityIdSpec(dataModelIdAndEntityIdSpec);
        return tempusResourceCriteriaSpec;
    }

    private Supplier<Stream<UserPermission>> getReadableAndResourceAccessibleStream(SecurityUser user, EntityType entityType) {
        return () -> user.getUserPermissions()
                .stream()
                .filter(userPermission -> userPermission.getUserActions().contains(UserAction.READ))
                .filter(userPermission -> userPermission.getResources().contains(entityType));
    }

    private Set<EntityId> getAccessibleResourceIdsForGivenDmo(Supplier<Stream<UserPermission>> userPermissionStream, EntityType entityType, DataModelObjectId dataModelObjectId) {
        return userPermissionStream.get()
                .map(UserPermission::getResourceAttributes)
                .filter(Objects::nonNull)
                .filter(resourceAttributes -> hasResourceIdsForGivenDmo(dataModelObjectId, resourceAttributes))
                .map(resourceAttributes -> getResourceIdByType(entityType, toUUID(resourceAttributes.get(UserPermission.ResourceAttribute.ID))))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private EntityId getResourceIdByType(EntityType entityType, UUID resourceUUID) {
        if(entityType.equals(EntityType.ASSET)){
            return new AssetId(resourceUUID);
        } else if(entityType.equals(EntityType.DEVICE)){
            return new DeviceId(resourceUUID);
        }
        return null;
    }

    private boolean hasResourceIdsForGivenDmo(DataModelObjectId dataModelObjectId, Map<UserPermission.ResourceAttribute, String> resourceAttributes) {
        if(resourceAttributes.containsKey(UserPermission.ResourceAttribute.DATA_MODEL_ID)
                && resourceAttributes.containsKey(UserPermission.ResourceAttribute.ID)){
            final DataModelObjectId accessibleDmoid = new DataModelObjectId(toUUID(resourceAttributes.get(UserPermission.ResourceAttribute.DATA_MODEL_ID)));
            return dataModelObjectId.equals(accessibleDmoid);
        }
        return false;
    }

    private boolean hasHierarchicalPermOnEntireDataModel(Supplier<Stream<UserPermission>> userPermissionStream, DataModelObjectId dataModelObjectId) {
        Set<DataModelObjectId> parentDataModelIds = dataModelObjectService.getAllParentDataModelIdsOf(dataModelObjectId);
        return userPermissionStream.get().map(UserPermission::getResourceAttributes).filter(Objects::nonNull).anyMatch(resourceAttributes -> {
                boolean isAccessedToDmoidOnly = resourceAttributes.containsKey(UserPermission.ResourceAttribute.DATA_MODEL_ID)
                    && !resourceAttributes.containsKey(UserPermission.ResourceAttribute.ID);

                if(isAccessedToDmoidOnly){
                    final DataModelObjectId accessibleDmoid = new DataModelObjectId(toUUID(resourceAttributes.get(UserPermission.ResourceAttribute.DATA_MODEL_ID)));
                    return dataModelObjectId.equals(accessibleDmoid) || parentDataModelIds.contains(accessibleDmoid);
            }
            return false;
        });
    }

    private boolean hasPermOnDmoWithResources(Supplier<Stream<UserPermission>> userPermissionStream, DataModelObjectId dataModelObjectId) {

        return userPermissionStream.get().map(UserPermission::getResourceAttributes).filter(Objects::nonNull).anyMatch(resourceAttributes -> {
            boolean isDmoidPresent = resourceAttributes.containsKey(UserPermission.ResourceAttribute.DATA_MODEL_ID);
            if(isDmoidPresent){
                final DataModelObjectId accessibleDmoid = new DataModelObjectId(toUUID(resourceAttributes.get(UserPermission.ResourceAttribute.DATA_MODEL_ID)));
                return dataModelObjectId.equals(accessibleDmoid);
            }
            return false;
        });
    }

    protected String getUploadedFilePath(MultipartFile file, String uploadPath) throws Exception {
        try(Stream<Path> filesStream = Files.list(Paths.get(uploadPath))) {
            List<String> filesAtDestination = filesStream.map(f -> f.getFileName().toString()).collect(Collectors.toList());
            if(filesAtDestination.contains(file.getOriginalFilename())) {
                throw new TempusException("Cant upload the same computation artifact again. Delete the existing computation first to upload it again" , TempusErrorCode.GENERAL);
            }
            String path = uploadPath + File.separator + file.getOriginalFilename();
            File destinationFile = new File(path);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, Paths.get(destinationFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
            }
            return path;
        } catch (Exception e) {
            throw e;
        }
    }

    protected boolean isZip(MultipartFile file) throws IOException {
        return Objects.requireNonNull(file.getOriginalFilename()).endsWith(".zip") ;
    }

}
