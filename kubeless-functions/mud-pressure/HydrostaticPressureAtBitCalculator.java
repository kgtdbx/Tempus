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
package io.kubeless;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hashmapinc.tempus.InputParserUtility;
import com.hashmapinc.tempus.MqttConnector;
import io.kubeless.Context;
import io.kubeless.Event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 * Mud Pressure (in N/m2) = Mud Weight (in N/m3) x Bit Depth (in m)
 * */
public class HydrostaticPressureAtBitCalculator {

    private static final String MQTT_URL = "tcp://tempus.hashmapinc.com:1883";
    private static final String ACCESS_TOKEN = "DEVICE_GATEWAY_TOKEN";

    private class Data {
        private String id;
        private Double mudWeight;
        private Double ds;
    }

    public String calculate(io.kubeless.Event event, io.kubeless.Context context) {
        String inputJson = event.Data;
        try {
            if (new InputParserUtility().validateJson(inputJson, Collections.singletonList("mudWeight"))) {
                Data inputData = new Gson().fromJson(inputJson, Data.class);

                double mudPressure = inputData.mudWeight * inputData.ds;

                Map<String, Double> data = new HashMap<String, Double>();
                data.put("HYDPRESSBIT", mudPressure);

                String json = new GsonBuilder().create().toJson(data);
                Optional<Long> empty = Optional.empty();
                new MqttConnector(MQTT_URL, ACCESS_TOKEN).publish(json, empty, Optional.of(inputData.ds), "mudPressure_" + inputData.id);
                return json;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
}