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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hashmapinc.server.common.data.CustomerGroup;
import com.hashmapinc.server.common.data.EntityType;
import com.hashmapinc.server.common.data.User;
import com.hashmapinc.server.common.data.audit.ActionType;
import com.hashmapinc.server.common.data.id.CustomerGroupId;
import com.hashmapinc.server.common.data.id.CustomerId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.common.data.id.UserId;
import com.hashmapinc.server.common.data.page.TextPageData;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.common.data.exception.TempusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
@Slf4j
public class CustomerGroupController extends BaseController {

    public static final String CUSTOMER_GROUP_ID = "customerGroupId";
    public static final String USER_ID = "userId";

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/group/{customerGroupId}")
    @ResponseBody
    public CustomerGroup getCustomerGroupById(@PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try {
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            return checkCustomerGroupId(customerGroupId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/group/{customerGroupId}/shortInfo")
    @ResponseBody
    public JsonNode getShortCustomerGroupInfoById(@PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try {
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            CustomerGroup customerGroup = checkCustomerGroupId(customerGroupId);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode infoObject = objectMapper.createObjectNode();
            infoObject.put("title", customerGroup.getTitle());
            ArrayNode policies = objectMapper.valueToTree(customerGroup.getPolicies());
            infoObject.putArray("policies").add(policies);
            return infoObject;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/group/{customerGroupId}/title", produces = "application/text")
    @ResponseBody
    public String getCustomerTitleById(@PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try {
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            CustomerGroup customerGroup = checkCustomerGroupId(customerGroupId);
            return customerGroup.getTitle();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/group")
    @ResponseBody
    public CustomerGroup saveCustomerGroup(@RequestBody CustomerGroup customerGroup) throws TempusException {
        try {
            customerGroup.setTenantId(getCurrentUser().getTenantId());
            customerGroup.setCustomerId(customerGroup.getCustomerId());
            CustomerGroup savedCustomerGroup = checkNotNull(customerGroupService.saveCustomerGroup(customerGroup));

            logEntityAction(savedCustomerGroup.getId(), savedCustomerGroup,
                    savedCustomerGroup.getCustomerId(),
                    customerGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedCustomerGroup;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.CUSTOMER_GROUP), customerGroup,
                    null, customerGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/group/{customerGroupId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomerGroup(@PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try {
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            CustomerGroup customerGroup = checkCustomerGroupId(customerGroupId);
            customerGroupService.deleteCustomerGroup(customerGroupId);

            logEntityAction(customerGroupId, customerGroup,
                    customerGroup.getCustomerId(),
                    ActionType.DELETED, null, strCustomerGroupId);

        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.CUSTOMER_GROUP),
                    null,
                    null,
                    ActionType.DELETED, e, strCustomerGroupId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{customerId}/groups", params = {"limit"})
    @ResponseBody
    public TextPageData<CustomerGroup> getCustomerGroups(@PathVariable("customerId") String strCustomerId,
                                               @RequestParam int limit,
                                               @RequestParam(required = false) String textSearch,
                                               @RequestParam(required = false) String idOffset,
                                               @RequestParam(required = false) String textOffset) throws TempusException {
        checkParameter("customerId", strCustomerId);
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            return checkNotNull(customerGroupService.findCustomerGroupsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{customerId}/group/{customerGroupTitle}")
    @ResponseBody
    public CustomerGroup getCustomerGroupByTitle(
            @PathVariable("customerId") String strCustomerId,
            @PathVariable("customerGroupTitle") String customerGroupTitle) throws TempusException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            return checkNotNull(customerGroupService.findCustomerByTenantIdAndCustomerIdAndTitle(tenantId, customerId, customerGroupTitle));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/group/{customerGroupId}/users", params = { "limit" })
    @ResponseBody
    public TextPageData<User> getUsersByGropuId(
            @PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try {
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            final CustomerGroup customerGroup = checkNotNull(checkCustomerGroupId(customerGroupId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return userService.findUsersByIds(customerGroup.getUserIds(), pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/group/{customerGroupId}/users")
    @ResponseBody
    public CustomerGroup assignUsersToGroup(
            @PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId,
            @RequestBody List<UUID> userUuids) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try{
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            List<UserId> userIds = userUuids.stream().map(UserId::new).collect(Collectors.toList());
            for (UserId userId: userIds) {
                checkUserId(userId);
            }
            return checkNotNull(customerGroupService.assignUsers(customerGroupId, userIds));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PutMapping(value = "/group/{customerGroupId}/users")
    @ResponseBody
    public CustomerGroup unassignUsersFromGroup(
            @PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId,
            @RequestBody List<UUID> userUuids) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try{
            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            List<UserId> userIds = userUuids.stream().map(UserId::new).collect(Collectors.toList());
            for (UserId userId: userIds) {
                checkUserId(userId);
            }
            return checkNotNull(customerGroupService.unassignUsers(customerGroupId, userIds));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/group/policy/{userId}")
    @ResponseBody
    public List<String> getPoliciesForUser(
            @PathVariable(USER_ID) String strUserId) throws TempusException {
        checkParameter(USER_ID, strUserId);
        try{

            UserId userId = new UserId(toUUID(strUserId));
            checkUserId(userId);
            return checkNotNull(customerGroupService.findGroupPoliciesForUser(userId));
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/group/{customerGroupId}/policy")
    @ResponseBody
    public Map<String, Map<String, String>> getDisplayablePoliciesForGroup(
            @PathVariable(CUSTOMER_GROUP_ID) String strCustomerGroupId) throws TempusException {
        checkParameter(CUSTOMER_GROUP_ID, strCustomerGroupId);
        try{

            CustomerGroupId customerGroupId = new CustomerGroupId(toUUID(strCustomerGroupId));
            return checkNotNull(customerGroupService.findGroupPolicies(customerGroupId));
        } catch (Exception e){
            throw handleException(e);
        }
    }
}
