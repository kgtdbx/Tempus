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
package com.hashmapinc.server.dao.service.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.hashmapinc.server.common.data.AssetLandingInfo;
import com.hashmapinc.server.common.data.Dashboard;
import com.hashmapinc.server.common.data.DashboardType;
import com.hashmapinc.server.common.data.id.DashboardId;
import com.hashmapinc.server.common.data.id.DataModelId;
import com.hashmapinc.server.common.data.id.DataModelObjectId;
import com.hashmapinc.server.dao.service.DaoSqlTest;
import com.hashmapinc.server.dao.service.BaseDashboardServiceTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@DaoSqlTest
public class DashboardServiceSqlTest extends BaseDashboardServiceTest {
}
