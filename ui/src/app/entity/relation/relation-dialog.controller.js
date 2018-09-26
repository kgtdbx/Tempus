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
import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/theme/github';
import beautify from 'js-beautify';

import './relation-dialog.scss';

const js_beautify = beautify.js;

/*@ngInject*/
export default function RelationDialogController($scope, $mdDialog, types, entityRelationService, isAdd, direction, relation, showingCallback, entityDetail, assetService, deviceService,$log) {

    var vm = this;

    vm.types = types;
    vm.isAdd = isAdd;
    vm.direction = direction;
    vm.entityDetail = entityDetail;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element);
    }

    vm.relationAdditionalInfoOptions = {
        useWrapMode: false,
        mode: 'json',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.editor = _ace;
        }
    };

    vm.relation = relation;
    if (vm.isAdd) {
        vm.relation.type = types.entityRelationType.contains;
        vm.targetEntityId = {};
    } else {
        if (vm.direction == vm.types.entitySearchDirection.from) {
            vm.targetEntityId = vm.relation.to;
        } else {
            vm.targetEntityId = vm.relation.from;
        }
    }

    vm.save = save;
    vm.cancel = cancel;

    vm.additionalInfo = '';

    if (vm.relation.additionalInfo) {
        vm.additionalInfo = angular.toJson(vm.relation.additionalInfo);
        vm.additionalInfo = js_beautify(vm.additionalInfo, {indent_size: 4});
    }

    $scope.$watch('vm.additionalInfo', () => {
        $scope.theForm.$setValidity("additionalInfo", true);
    });

    function updateEditorSize(element) {
        var newHeight = 200;
        angular.element('#tb-relation-additional-info', element).height(newHeight.toString() + "px");
        vm.editor.resize();
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        if (vm.isAdd) {
            if (vm.direction == vm.types.entitySearchDirection.from) {
                vm.relation.to = vm.targetEntityId;
            } else {
                vm.relation.from = vm.targetEntityId;
            }
        }
        $scope.theForm.$setPristine();

        var valid = true;
        if (vm.additionalInfo && vm.additionalInfo.length) {
            try {
                vm.relation.additionalInfo = angular.fromJson(vm.additionalInfo);
            } catch(e) {
                valid = false;
            }
        } else {
            vm.relation.additionalInfo = null;
        }

        $scope.theForm.$setValidity("additionalInfo", valid);

        $log.log(relation);
        $log.log(vm.relation)
        if (valid) {
            entityRelationService.saveRelation(vm.relation).then(
                function success() {
                    if((relation.from.entityType == 'DEVICE' || relation.from.entityType == 'ASSET' )&& vm.entityDetail && vm.entityDetail.customerId.id == '13814000-1dd2-11b2-8080-808080808080'){
                            assetService.getAsset(vm.relation.to.id).then(function success(response){
                                $log.log(response)

                                if(response){
                                    if(relation.to.entityType == 'ASSET' && relation.from.entityType == 'DEVICE'){
                                        deviceService.assignDeviceToCustomer(response.customerId.id,vm.entityDetail.id.id).then();
                                    }else if(relation.to.entityType == 'ASSET' && relation.from.entityType == 'ASSET'){
                                        assetService.assignAssetToCustomer(response.customerId.id,vm.entityDetail.id.id).then();
                                    }

                                }
                            });
                    }
                    $mdDialog.hide();
                }
            );
        }
    }

}
