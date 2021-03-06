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
package com.hashmapinc.server.requests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashmapinc.server.common.data.User;
import com.hashmapinc.server.common.data.id.CustomerId;
import com.hashmapinc.server.common.data.id.TenantId;
import com.hashmapinc.server.common.data.id.UserId;
import com.hashmapinc.server.common.data.security.Authority;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hashmapinc.server.dao.model.ModelConstants.NULL_UUID;

@Data
public class IdentityUser {
    private UUID id;
    private String userName;
    private UUID tenantId;
    private UUID customerId;
    private String firstName;
    private String lastName;
    private List<String> authorities;
    private String clientId;
    private Map<String, Object> additionalDetails;
    private boolean enabled;

    public IdentityUser(){}

    public IdentityUser(User user){
        this.id = user.getUuidId();
        this.userName = user.getEmail();
        this.authorities = Arrays.asList(user.getAuthority().name());
        if(user.getTenantId() != null) {
            this.tenantId = user.getTenantId().getId();
        } else {
            this.tenantId = NULL_UUID;
        }

        if(user.getCustomerId() != null) {
            this.customerId = user.getCustomerId().getId();
        } else {
            this.customerId = NULL_UUID;
        }

        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.enabled = user.isEnabled();
        this.additionalDetails = convertJsonNode(user.getAdditionalInfo());
    }

    private Map<String,Object> convertJsonNode(JsonNode additionalInfo) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> additionalDetails = mapper.convertValue(additionalInfo, Map.class);
        return  additionalDetails;
    }

    private JsonNode convertMapToJsonNode(Map<String, Object> additionalDetails){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNodeMap = mapper.convertValue(additionalDetails, JsonNode.class);
        return jsonNodeMap;
    }
    public User toUser(){
        User user = new User();
        user.setId(new UserId(id));
        user.setEmail(userName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if(tenantId != null) {
            user.setTenantId(new TenantId(tenantId));
        }
        if(customerId != null) {
            user.setCustomerId(new CustomerId(customerId));
        }
        user.setAuthority(Authority.parse(authorities.get(0)));
        user.setEnabled(enabled);
        user.setAdditionalInfo(convertMapToJsonNode(additionalDetails));
        return user;
    }
}
