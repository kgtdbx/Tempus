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
package com.hashmapinc.server.dao.settings;

import com.google.gson.JsonObject;
import com.hashmapinc.server.common.data.UserSettings;
import com.hashmapinc.server.common.data.id.UserId;
import com.hashmapinc.server.common.data.id.UserSettingsId;
import com.hashmapinc.server.dao.service.DataValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hashmapinc.server.dao.exception.DataValidationException;
import com.hashmapinc.server.dao.service.Validator;


@Service
@Slf4j
public class UserSettingsServiceImpl implements UserSettingsService {
    
    @Autowired
    private UserSettingsDao userSettingsDao;

    private static final String INCORRECT_USERID = "Incorrect UserId ";
    private static final String DEFAULT_UNIT_SYSTEM = "SI";

    @Override
    public UserSettings findUserSettingsById(UserSettingsId userSettingsId) {
        log.trace("Executing findUserSettingsById [{}]", userSettingsId);
        Validator.validateId(userSettingsId, "Incorrect userSettingsId " + userSettingsId);
        return userSettingsDao.findById(userSettingsId.getId());
    }

    @Override
    public UserSettings findUserSettingsByKeyAndUserId(String key, UserId userId) {
        log.trace("Executing findUserSettingsByKeyAndUserId, key [{}], userId [{}]", key, userId);
        Validator.validateString(key, "Incorrect key " + key);
        return userSettingsDao.findByKeyAndUserId(key, userId.getId());
    }

    @Override
    public UserSettings saveUserSettings(UserSettings userSettings) {
        log.trace("Executing saveUserSettings [{}]", userSettings);
        adminSettingsValidator.validate(userSettings);
        return userSettingsDao.save(userSettings);
    }

    @Override
    public void saveUnitSystem(String unitSystem , UserId userId) {
        log.trace("Executing saveUnitSystem, unitSystem [{}], userId [{}]", unitSystem, userId);
        Validator.validateEntityId(userId, INCORRECT_USERID + userId);
        String foundUnitSystem = userSettingsDao.findUnitSystemByUserId(userId.getId());
        if (foundUnitSystem == null) {
            userSettingsDao.saveUnitSystem(unitSystem , userId.getId());
        } else if (!foundUnitSystem.equals(unitSystem)) {
            userSettingsDao.updateUnitSystem(unitSystem, userId.getId());
        }
    }

    @Override
    public String findUnitSystemByUserId(UserId userId) {
        log.trace("Executing findUnitSystemByUserId, userId [{}]", userId);
        Validator.validateEntityId(userId, INCORRECT_USERID + userId);
        String unitSystemByUserId = userSettingsDao.findUnitSystemByUserId(userId.getId());
        if (unitSystemByUserId == null) {
            unitSystemByUserId = DEFAULT_UNIT_SYSTEM;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("unit-system",unitSystemByUserId);
        return jsonObject.toString();
    }

    @Override
    public void deleteUnitSystemByUserId(UserId userId) {
        log.trace("Executing deleteUnitSystemByUserId, userId [{}]", userId);
        Validator.validateEntityId(userId, INCORRECT_USERID + userId);
        userSettingsDao.deleteUnitSystemByUserId(userId.getId());
    }

    private DataValidator<UserSettings> adminSettingsValidator =
            new DataValidator<UserSettings>() {

                @Override
                protected void validateCreate(UserSettings userSettings) {
                    UserSettings existentUserSettingsWithKey = findUserSettingsByKeyAndUserId(userSettings.getKey(), userSettings.getUserId());
                    if (existentUserSettingsWithKey != null) {
                        throw new DataValidationException("User settings with such name already exists for this user!");
                    }
                }

                @Override
                protected void validateUpdate(UserSettings userSettings) {
                    UserSettings existentUserSettings = findUserSettingsById(userSettings.getId());
                    if (existentUserSettings != null) {
                        if (!existentUserSettings.getKey().equals(userSettings.getKey())) {
                            throw new DataValidationException("Changing key of user settings entry is prohibited!");
                        }
                        validateJsonStructure(existentUserSettings.getJsonValue(), userSettings.getJsonValue());
                    }
                }

        
                @Override
                protected void validateDataImpl(UserSettings userSettings) {
                    if (StringUtils.isEmpty(userSettings.getKey())) {
                        throw new DataValidationException("Key should be specified!");
                    }
                    if (userSettings.getJsonValue() == null) {
                        throw new DataValidationException("Json value should be specified!");
                    }
                }
    };

}
