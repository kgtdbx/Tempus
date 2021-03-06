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
package com.hashmapinc.server.dao.datamodel;

import com.hashmapinc.server.common.data.datamodel.DataModelObject;
import com.hashmapinc.server.common.data.id.DataModelId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;

import java.util.List;
import java.util.UUID;

public interface DataModelObjectDao {
    DataModelObject findById(DataModelObjectId id);
    DataModelObject save(DataModelObject dataModelObject);
    List<DataModelObject> findByDataModelId(DataModelId dataModelId);
    List<DataModelObject> findByDataModelIdAndType(DataModelId dataModelId, String type);
    DataModelObject findByDataModeIdAndName(DataModelObject dataModelObject);
    boolean removeById(UUID id);
    List<DataModelObject> findByName(String name);
}
