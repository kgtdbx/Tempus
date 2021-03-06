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

import tenantFieldsetTemplate from './tenant-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function TenantDirective($compile, $templateCache, $translate, toast) {
    var linker = function (scope, element) {
        var template = $templateCache.get(tenantFieldsetTemplate);
        element.html(template);

        scope.onTenantIdCopied = function() {
            toast.showSuccess($translate.instant('tenant.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.fileAdded = function($file) {
            var reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        var addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            if($file.getExtension() === 'png' || $file.getExtension() === 'jpeg' || $file.getExtension() === 'svg' || $file.getExtension() === 'jpg'){
                                scope.tenant.logo = addedFile;
                                scope.tenant.logoFileName = $file.name;
                            }
                        }
                    }
                });
            };
            reader.readAsDataURL($file.file);

        };

        scope.clearFile = function() {
            scope.tenant.logoFileName = null;
            scope.tenant.logo = null;
        }

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            tenant: '=',
            isEdit: '=',
            theForm: '=',
            onManageUsers: '&',
            onDeleteTenant: '&'
        }
    };
}
