/*
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
/* eslint-disable import/no-unresolved, import/default */

import wellTrackComponents from './well-log-component.tpl.html'

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WellLogViewerComponentsDirective($compile, $templateCache,types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(wellTrackComponents);
        element.html(template);
        scope.gridTypes = types.wellLogComponent.gridTypes;
        scope.showConfigParam =  false;
        scope.$watch('datasources', function() {
           scope.extractDataKeys();
        }, true);
        scope.componentTypes = types.wellLogComponent.componentTypes;
        scope.styleTypes = types.wellLogComponent.styleTypes;

        scope.fillTypes = function() {
            if(scope.trackComponent.lines.length == 1) {
                return types.wellLogComponent.fillTypes;
            }
            if(scope.trackComponent.lines.length == 2) {
                return types.wellLogComponent.allFillTypes;
            }
        }
        scope.lines = function () {
            return scope.trackComponent.lines ? scope.trackComponent.lines : [];
        };

        scope.removeLine = function ($event,id){
            var index = scope.trackComponent.lines.findIndex(x => x.id==id);
            if($event){
                  $event.stopPropagation();
                  $event.preventDefault();
            }
            scope.trackComponent.lines.splice(index, 1);
        };

        scope.addLine = function (){
            var lines = scope.lines()
            var line = {
                id: lines.length ? lines.length + 1 : 1,
                cType: 'Line'
            }
            scope.trackComponent.lines ? scope.trackComponent.lines.push(line) : scope.trackComponent.lines = [line];
        };

        scope.extractDataKeys = function() {
            scope.datasourcesList = [];
            scope.datasources.forEach(function(dataSources){
                dataSources.value.dataKeys.forEach(function(keys){
                    scope.datasourcesList.push(keys)
                })
            })
        };
        scope.extractDataKeys();
        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            components: '=',
            trackComponent: '=',
            datasources:'='
        }
    };
}