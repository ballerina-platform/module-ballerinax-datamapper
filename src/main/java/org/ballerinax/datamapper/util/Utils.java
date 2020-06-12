/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.datamapper.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Utils {
    public static void writeToFile(String content, Path targetFilePath) throws IOException {
        File newFile = targetFilePath.toFile();

        if (newFile.exists()) {
            Files.write(targetFilePath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }

        if (newFile.getParentFile().mkdirs()) {
            Files.write(targetFilePath, content.getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.write(targetFilePath, content.getBytes(StandardCharsets.UTF_8));
    }
}
