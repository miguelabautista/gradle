/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.nativeplatform

import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.gradle.util.TemporaryFolder

import org.junit.Rule
import spock.lang.Specification

class CommonFileSystemTest extends Specification {
    @Rule TemporaryFolder tmpDir = new TemporaryFolder()
    def fs = FileSystems.default
    def posix = PosixUtil.current()

    def "unix permissions cannot be read on non existing file"() {
        when:
        fs.getUnixMode(tmpDir.file("someFile"))

        then:
        thrown(FileNotFoundException)
    }

    @Requires(TestPrecondition.FILE_PERMISSIONS)
    def "unix permissions on files can be changed and read"() {
        def f = tmpDir.createFile("someFile")

        when:
        fs.chmod(f, mode)

        then:
        fs.getUnixMode(f) == mode
        (posix.stat(f.getAbsolutePath()).mode() & 0777) == mode

        where:
        mode << [0644, 0600]
    }

    @Requires(TestPrecondition.FILE_PERMISSIONS)
    def "unix permissions on directories can be changed and read"() {
        def d = tmpDir.createDir("someDir")

        when:
        fs.chmod(d, mode)

        then:
        fs.getUnixMode(d) == mode
        (posix.stat(d.getAbsolutePath()).mode() & 0777) == mode

        where:
        mode << [0755, 0700]
    }
}
