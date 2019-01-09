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
package com.hashmapinc.server.dao.sql.template;

import com.hashmapinc.server.common.data.UUIDConverter;
import com.hashmapinc.server.common.data.page.TextPageLink;
import com.hashmapinc.server.common.data.template.TemplateMetadata;
import com.hashmapinc.server.dao.DaoUtil;
import com.hashmapinc.server.dao.model.ModelConstants;
import com.hashmapinc.server.dao.model.sql.TemplateMetadataEntity;
import com.hashmapinc.server.dao.sql.JpaAbstractSearchTextDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class JpaBaseTemplateDao extends JpaAbstractSearchTextDao<TemplateMetadataEntity, TemplateMetadata> {

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    protected Class<TemplateMetadataEntity> getEntityClass() {
        return TemplateMetadataEntity.class;
    }

    @Override
    protected CrudRepository<TemplateMetadataEntity, String> getCrudRepository() {
        return templateRepository;
    }

    public List<TemplateMetadata> findByPageLink(TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                templateRepository.findTemplate(Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                                PageRequest.of(0, pageLink.getLimit())));

    }
}
