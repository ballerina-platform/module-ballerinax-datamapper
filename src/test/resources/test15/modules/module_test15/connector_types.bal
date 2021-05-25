// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

public type Issue record {|
    string id = "";
    string? bodyText = "";
    string? closed = "";
    string? closedAt = "";
    string createdAt = "";
    Creator author = {};
    Label[] labels = [];
    int number = 0;
    string state = "";
    string title = "";
    string? updatedAt = "";
    string url = "";
    Assignee[] assignees = [];
|};

public type Assignee record {|
    string id = "";
    string login = "";
    string email = "";
    string? bio = "";
    string? url = "";
|};

public type Creator record {|
    string login = "";
    string? resourcePath = "";
    string? url = "";
    string? avatarUrl = "";
|};

public type Label record {|
    string id = "";
    string name = "";
    string? description = "";
    string? color = "";
|};
