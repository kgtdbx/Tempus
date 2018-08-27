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
/*@ngInject*/
export default function AssignGroupsToUserController($scope, toast, $translate, userService, userGroupService, $mdDialog, userId, groups, customerId) {

    var vm = this;

    vm.groups = groups;
    vm.groups.selections = [];
    vm.searchText = '';
    vm.groups.selectedCount = 0;

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchUserTextUpdated = searchUserTextUpdated;
    vm.toggleUserSelection = toggleUserSelection;
    vm.customerId = customerId;
    vm.groupId = userId;
    vm.groupName = [];
    //vm.email =[];
    $scope.assignedGroups = function() {
        var pageSize = 100;
        userGroupService.assignedGroups(vm.groupId, {
            limit: pageSize,
            textSearch: ''
        }).then(
            function success(assigngroups) {

                if (assigngroups.data.length > 0) {
                    angular.forEach(assigngroups.data, function(value) {

                        vm.groupName.push(value.name);
                    });
                    vm.groups.data = vm.groups.data.filter(function(e) {
                        return vm.groupName.indexOf(e.name) == -1;
                    });
                    vm.groups.selections = [];
                }
            },
            function fail() {

            });
    };


    $scope.assignedGroups();

    vm.theGroups = {
        getItemAtIndex: function(index) {
            if (index > vm.groups.data.length) {
                vm.theGroups.fetchMoreItems_(index);
                return null;
            }
            var item = vm.groups.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }


            return item;
        },

        getLength: function() {
            if (vm.groups.hasNext) {
                return vm.groups.data.length + vm.groups.nextPageLink.limit;
            } else {
                return vm.groups.data.length;
            }
        },

        fetchMoreItems_: function() {
            if (vm.groups.hasNext && !vm.groups.pending) {
                vm.groups.pending = true;

                userGroupService.getGroups(vm.customerId, vm.groups.nextPageLink).then(
                    function success(groups) {
                        vm.groups.data = vm.groups.data.concat(groups.data);
                        $scope.assignedGroups();
                        vm.groups.nextPageLink = groups.nextPageLink;
                        vm.groups.hasNext = groups.hasNext;
                        if (vm.groups.hasNext) {
                            vm.groups.nextPageLink.limit = vm.groups.pageSize;
                        }
                        vm.groups.pending = false;
                    },
                    function fail() {
                        vm.groups.hasNext = false;
                        vm.groups.pending = false;
                    });
            }
        }
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        //var tasks = [];

        userGroupService.assignGroupToUser(vm.groupId, vm.groups.selections).then(

            function success() {

                $mdDialog.cancel();
                toast.showSuccess($translate.instant('user.user-assigned-message'));

            },

            function fail() {});

    }

    // vm.assignedUsers();

    function noData() {
        return vm.groups.data.length == 0 && !vm.groups.hasNext;
    }

    function hasData() {
        return vm.groups.data.length > 0;
    }

    function toggleUserSelection($event, user) {

        $event.stopPropagation();
        var selected = angular.isDefined(user.selected) && user.selected;

        user.selected = !selected;
        if (user.selected) {
            vm.groups.selections.push(user.id.id);
            vm.groups.selectedCount++;
        } else {

            var index = vm.groups.selections.indexOf(user.id.id)
            vm.groups.selections.splice(index, 1);
            vm.groups.selectedCount--;
        }
    }


    function searchUserTextUpdated() {
        vm.groups = {
            pageSize: vm.groups.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.groups.pageSize,
                textSearch: vm.searchText
            },
            selections: [],
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }

}