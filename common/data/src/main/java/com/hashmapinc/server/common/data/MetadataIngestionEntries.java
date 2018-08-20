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
package com.hashmapinc.server.common.data;

import com.hashmapinc.server.common.data.kv.MetaDataKvEntry;

import java.util.List;

public class MetadataIngestionEntries {
    private String tenantId;
    private String metadataConfigId;
    private String metadataSourceName;
    private List<MetaDataKvEntry> metaDataKvEntries;


    public MetadataIngestionEntries(String tenantId, String metadataConfigId, String metadataSourceName, List<MetaDataKvEntry> metaDataKvEntries) {
        this.tenantId = tenantId;
        this.metadataConfigId = metadataConfigId;
        this.metaDataKvEntries = metaDataKvEntries;
        this.metadataSourceName = metadataSourceName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getMetadataConfigId() {
        return metadataConfigId;
    }

    public void setMetadataConfigId(String metadataConfigId) {
        this.metadataConfigId = metadataConfigId;
    }

    public String getMetadataSourceName() {
        return metadataSourceName;
    }

    public void setMetadataSourceName(String metadataSourceName) {
        this.metadataSourceName = metadataSourceName;
    }

    public List<MetaDataKvEntry> getMetaDataKvEntries() {
        return metaDataKvEntries;
    }

    public void setMetaDataKvEntries(List<MetaDataKvEntry> metaDataKvEntries) {
        this.metaDataKvEntries = metaDataKvEntries;
    }
}