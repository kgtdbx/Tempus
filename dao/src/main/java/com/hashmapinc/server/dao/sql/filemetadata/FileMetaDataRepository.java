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
package com.hashmapinc.server.dao.sql.filemetadata;


import com.hashmapinc.server.dao.model.sql.FileMetaDataCompositeKey;
import com.hashmapinc.server.dao.model.sql.FileMetaDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface FileMetaDataRepository extends CrudRepository<FileMetaDataEntity, FileMetaDataCompositeKey> {
    List<FileMetaDataEntity> findByTenantIdAndRelatedEntityId(String tenantId, String entityId);
    Page<FileMetaDataEntity> findByTenantIdAndRelatedEntityIdAndFileNameStartingWithIgnoreCase(String tenantId, String entityId, String searchText, Pageable pageable);
}
