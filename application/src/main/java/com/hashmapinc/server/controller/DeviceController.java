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

import com.google.common.util.concurrent.ListenableFuture;
import com.hashmapinc.server.common.data.*;
import com.hashmapinc.server.common.data.audit.ActionType;
import com.hashmapinc.server.common.data.device.DeviceSearchQuery;
import com.hashmapinc.server.common.data.id.CustomerId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;
import com.hashmapinc.server.common.data.id.DeviceId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.common.data.kv.AttributeKvEntry;
import com.hashmapinc.server.common.data.page.PaginatedResult;
import com.hashmapinc.server.common.data.security.Authority;
import com.hashmapinc.server.common.data.security.DeviceCredentials;
import com.hashmapinc.server.dao.attributes.AttributesService;
import com.hashmapinc.server.dao.depthseries.DepthSeriesService;
import com.hashmapinc.server.dao.device.DeviceCredentialsService;
import com.hashmapinc.server.dao.exception.IncorrectParameterException;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.timeseries.TimeseriesService;
import com.hashmapinc.server.common.data.exception.TempusErrorCode;
import com.hashmapinc.server.common.data.exception.TempusException;
import com.hashmapinc.server.service.security.model.SecurityUser;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class DeviceController extends BaseController {

    public static final String DEVICE_ID = "deviceId";
    public static final String DATA_MODEL_OBJECT_ID = "dataModelObjectId";


    @Autowired
    protected TimeseriesService timeseriesService;

    @Autowired
    protected DepthSeriesService depthSeriesService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @PostAuthorize("hasPermission(returnObject, 'DEVICE_READ')")
    @GetMapping(value = "/device/{deviceId}")
    @ResponseBody
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceId(deviceId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#device, 'DEVICE_CREATE') or hasPermission(#device, 'DEVICE_UPDATE')")
    @PostMapping(value = "/device")
    @ResponseBody
    public Device saveDevice(@RequestBody Device device) throws TempusException {
        try {
            device.setTenantId(getCurrentUser().getTenantId());
            if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                checkCustomerId(device.getCustomerId());
            }
            Device savedDevice = checkNotNull(deviceService.saveDevice(device));

            actorService
                    .onDeviceNameOrTypeUpdate(
                            savedDevice.getTenantId(),
                            savedDevice.getId(),
                            savedDevice.getName(),
                            savedDevice.getType());

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_DELETE')")
    @DeleteMapping(value = "/device/{deviceId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            deviceService.deleteDevice(deviceId);

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, strDeviceId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_ASSIGN')")
    @PostMapping(value = "/customer/{customerId}/device/{deviceId}")
    @ResponseBody
    public Device assignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId);

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            checkDeviceId(deviceId);

            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(deviceId, customerId));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId, strCustomerId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_ASSIGN')")
    @DeleteMapping(value = "/customer/device/{deviceId}")
    @ResponseBody
    public Device unassignDeviceFromCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(device.getCustomerId());

            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(deviceId));

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strDeviceId, customer.getId().toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_ASSIGN')")
    @PostMapping(value = "/customer/public/device/{deviceId}")
    @ResponseBody
    public Device assignDeviceToPublicCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(device.getTenantId());
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(deviceId, publicCustomer.getId()));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_READ')")
    @GetMapping(value = "/device/{deviceId}/credentials")
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@PathVariable(DEVICE_ID) String strDeviceId) throws TempusException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(deviceId));
            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, strDeviceId);
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission((#deviceCredentials.getDeviceId() != null ? #deviceCredentials.getDeviceId().getId().toString() : null), 'DEVICE', 'DEVICE_UPDATE')")
    @PostMapping(value = "/device/credentials")
    @ResponseBody
    public DeviceCredentials saveDeviceCredentials(@RequestBody DeviceCredentials deviceCredentials) throws TempusException {
        checkNotNull(deviceCredentials);
        try {
            Device device = checkDeviceId(deviceCredentials.getDeviceId());
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(deviceCredentials));
            actorService.onCredentialsUpdate(getCurrentUser().getTenantId(), deviceCredentials.getDeviceId());
            logEntityAction(device.getId(), device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/devices", params = {"limit"})
    @ResponseBody
    public PaginatedResult<Device> getTenantDevices(
            @RequestParam int limit,
            @RequestParam int pageNum,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch) throws TempusException {
        try {
            final TempusResourceCriteriaSpec tempusResourceCriteriaSpec = getTempusResourceCriteriaSpec(getCurrentUser(), EntityType.DEVICE, null, null, type, textSearch);
            return deviceService.findAll(tempusResourceCriteriaSpec, limit, pageNum);

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/tenant/devices", params = {"deviceName"})
    @ResponseBody
    public Device getTenantDevice(
            @RequestParam String deviceName) throws TempusException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/customer/{customerId}/devices", params = {"limit"})
    @ResponseBody
    public PaginatedResult<Device> getCustomerDevices(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam int pageNum,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch) throws TempusException {
        checkParameter("customerId", strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            final TempusResourceCriteriaSpec tempusResourceCriteriaSpec = getTempusResourceCriteriaSpec(getCurrentUser(), EntityType.DEVICE, null, customerId, type, textSearch);
            tempusResourceCriteriaSpec.setCustomerId(Optional.of(customerId));
            return deviceService.findAll(tempusResourceCriteriaSpec, limit, pageNum);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostFilter("hasPermission(filterObject, 'DEVICE_READ')")
    @GetMapping(value = "/devices", params = {"deviceIds"})
    @ResponseBody
    public List<Device> getDevicesByIds(
            @RequestParam("deviceIds") String[] strDeviceIds) throws TempusException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
            }
            return checkNotNull(devices.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_READ')")
    @GetMapping(value = "/download/deviceAttributesData")
    @ResponseBody
    public void downloadAttributesDataAsCsv(HttpServletResponse response, @RequestParam("deviceId") String strDeviceId) throws IOException {
        String csvFileName = "device_attributes_data.csv";
        response.setContentType("text/csv");
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", csvFileName);
        response.setHeader(headerKey, headerValue);

        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));

        CSVWriter csvWriter = new CSVWriter(response.getWriter(),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        DeviceDataSet deviceDataSet = attributesService.findAll(deviceId);

        csvWriter.writeNext(deviceDataSet.getHeaderRow());
        csvWriter.writeAll(deviceDataSet.getDataRows());
        csvWriter.close();
    }

    @PreAuthorize("hasPermission(#strDeviceId, 'DEVICE', 'DEVICE_READ')")
    @GetMapping(value = "/download/deviceSeriesData")
    @ResponseBody
    public void downloadTimeSeriesOrDepthSeriesDataAsCSV(HttpServletResponse response,
                            @RequestParam("deviceId") String strDeviceId,
                            @RequestParam("type") String dataSetType,
                            @RequestParam("startValue") String startValue,
                            @RequestParam("endValue") String endValue) throws TempusException, IOException {
        String csvFileName = "device_data.csv";
        response.setContentType("text/csv");
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", csvFileName);
        response.setHeader(headerKey, headerValue);

        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));

        CSVWriter csvWriter = new CSVWriter(response.getWriter(),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        DeviceDataSet deviceDataSet;
        if(dataSetType.equals("ts")) {
            deviceDataSet = timeseriesService.findAllBetweenTimeStamp(deviceId, checkLong(startValue, "startValue"), checkLong(endValue, "endValue"));
        } else if(dataSetType.equals("ds")) {
            deviceDataSet = depthSeriesService.findAllBetweenDepths(deviceId, checkDouble(startValue, "startValue"), checkDouble(endValue, "endValue"));
        } else {
            throw new TempusException("Unsupported Data Set Type Supplied", TempusErrorCode.BAD_REQUEST_PARAMS);
        }
        csvWriter.writeNext(deviceDataSet.getHeaderRow());
        csvWriter.writeAll(deviceDataSet.getDataRows());
        csvWriter.close();
    }


    @PostFilter("hasPermission(filterObject, 'DEVICE_READ')")
    @PostMapping(value = "/devices")
    @ResponseBody
    public List<Device> findByQuery(@RequestBody DeviceSearchQuery query) throws TempusException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId());
        try {
            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(query).get());
            devices = devices.stream().filter(device -> {
                try {
                    checkDevice(device);
                    return true;
                } catch (TempusException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return devices;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/device/types")
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes() throws TempusException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("#oauth2.isClient() and #oauth2.hasScope('server')")
    @GetMapping(value = "/device/{deviceId}/data-quality-meta-data", produces = "application/json")
    @ResponseBody
    public DataQualityMetaData getDataQualityMetaData(@PathVariable("deviceId") String strDeviceId) throws TempusException {
        try {
            checkParameter(DEVICE_ID, strDeviceId);

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            String metaData = "";
            Optional<AttributeKvEntry> attributeKvEntry = attributesService.find(deviceId, DataConstants.SHARED_SCOPE, "quality_meta_data").get();
            if (attributeKvEntry.isPresent())
                metaData = attributeKvEntry.get().getValueAsString();

            DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(deviceId);
            return new DataQualityMetaData(credentials.getCredentialsId(), metaData);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/datamodelobject/devices/{dataModelObjectId}")
    @ResponseBody
    public PaginatedResult<Device> getDevicesByDataModelObjectId(@PathVariable(DATA_MODEL_OBJECT_ID) String strDataModelObjectId,
                                                                 @RequestParam int limit,
                                                                 @RequestParam int pageNum,
                                                                 @RequestParam(required = false) String textSearch) throws TempusException {
        checkParameter(DATA_MODEL_OBJECT_ID, strDataModelObjectId);
        try {
            DataModelObjectId dataModelObjectId = new DataModelObjectId(toUUID(strDataModelObjectId));
            final TempusResourceCriteriaSpec tempusResourceCriteriaSpec = getTempusResourceCriteriaSpec(getCurrentUser(), EntityType.DEVICE, dataModelObjectId, null, null, textSearch);
            return deviceService.findAll(tempusResourceCriteriaSpec, limit, pageNum);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
