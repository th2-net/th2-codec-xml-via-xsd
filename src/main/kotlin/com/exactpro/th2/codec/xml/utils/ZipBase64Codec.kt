/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.th2.codec.xml.utils

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipBase64Codec {
    companion object {
        private val CHARSET: Charset = StandardCharsets.UTF_8

        fun decode(data: String, dir: File) : Map<String, Path> {
            return decode(data.toByteArray(CHARSET), dir)
        }

        fun decode(data: ByteArray, dir: File) : Map<String, Path> {
            val result = mutableMapOf<String, Path>()
            val bytes: ByteArray = Base64.getDecoder().decode(data)
            ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val newFile: File = newFile(dir, entry)
                    if (entry.isDirectory) {
                        if (!newFile.isDirectory && !newFile.mkdirs()) {
                            throw IOException("Failed to create directory $newFile")
                        }
                    } else {
                        // fix for Windows-created archives
                        val parent: File = newFile.parentFile
                        if (!parent.isDirectory && !parent.mkdirs()) {
                            throw IOException("Failed to create directory $parent")
                        }

                        // write file content
                        val copyTo = newFile.toPath()
                        Files.copy(zipIn, copyTo, StandardCopyOption.REPLACE_EXISTING)
                        result[entry.name] = copyTo
                    }
                    entry = zipIn.nextEntry
                }
            }
            return result
        }

        private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
            val destFile = File(destinationDir, zipEntry.name)
            val destDirPath = destinationDir.canonicalPath
            val destFilePath = destFile.canonicalPath
            if (!destFilePath.startsWith(destDirPath + File.separator)) {
                throw IOException("Entry is outside of the target dir: " + zipEntry.name)
            }
            return destFile
        }
    }
}