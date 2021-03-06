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
package org.gradle.api.internal.artifacts.ivyservice.filestore;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.gradle.api.GradleException;
import org.gradle.api.internal.artifacts.ivyservice.ArtifactCacheMetaData;
import org.gradle.util.hash.HashUtil;

import java.io.File;
import java.util.Random;

public class DefaultArtifactFileStore implements ArtifactFileStore {
    private static final String DEFAULT_ARTIFACT_PATTERN =
            "[organisation]/[module](/[branch])/[revision]/[type]/[sha1]/[artifact]-[revision](-[classifier])(.[ext])";
    public static final String EXTERNAL_VIEW_PATTERN = 
            "[organisation]/[module](/[branch])/[revision]/[type]/*/[artifact]-[revision](-[classifier])(.[ext])";

    private final File baseDir;
    private final Random generator = new Random(System.currentTimeMillis());

    public DefaultArtifactFileStore(ArtifactCacheMetaData cacheMetaData) {
        this.baseDir = new File(cacheMetaData.getCacheDir(), "filestore");
    }

    public File add(ArtifactRevisionId artifactId, File contentFile) {
        String checksum = getChecksum(contentFile);
        File storageFile = getArtifactFile(artifactId, checksum);
        if (!storageFile.exists()) {
            saveIntoFileStore(contentFile, storageFile);
        }
        return storageFile;
    }

    private void saveIntoFileStore(File contentFile, File storageFile) {
        if (!storageFile.getParentFile().exists()) {
            storageFile.getParentFile().mkdirs();
        }
        if (!contentFile.renameTo(storageFile)) {
            throw new GradleException(String.format("Failed to copy downloaded content into storage file: %s", storageFile));
        }
    }

    private String getChecksum(File contentFile) {
        return HashUtil.createHash(contentFile, "SHA1").asHexString();
    }

    private File getArtifactFile(ArtifactRevisionId artifactId, String sha1) {
        String artifactPath = getArtifactPath(artifactId, sha1);
        return new File(baseDir, artifactPath);
    }

    private String getArtifactPath(ArtifactRevisionId artifactId, String sha1) {
        Artifact dummyArtifact = new DefaultArtifact(artifactId, null, null, false);
        String substitute = IvyPatternHelper.substitute(DEFAULT_ARTIFACT_PATTERN, dummyArtifact);
        substitute = IvyPatternHelper.substituteToken(substitute, "sha1", sha1);
        return substitute;
    }

    public File getTempFile() {
        long tempLong = generator.nextLong();
        tempLong = tempLong < 0 ? -tempLong : tempLong;
        return new File(baseDir, "temp/" + tempLong);
    }

    public ExternalArtifactCache asExternalArtifactCache() {
        return new PatternBasedExternalArtifactCache(baseDir, EXTERNAL_VIEW_PATTERN);
    }
}
