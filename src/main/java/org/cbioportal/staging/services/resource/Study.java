package org.cbioportal.staging.services.resource;

import org.springframework.core.io.Resource;

public final class Study {

    private String studyId;
    private String version;
    private String timestamp;
    private Resource[] resources;
    private Resource studyDir;

    public Study(String studyId, String version, String timestamp, Resource studyDir, Resource[] resources) {
        this.studyId = studyId;
        this.version = version;
        this.timestamp = timestamp;
        this.studyDir = studyDir;
        this.resources = resources;
    }

    public String getStudyId() {
        return this.studyId;
    }

    public String getVersion() {
        return this.version;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public Resource[] getResources() {
        return this.resources;
    }

    public Resource getStudyDir() {
        return this.studyDir;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return "{" +
            " studyId='" + getStudyId() + "'" +
            ", version='" + getVersion() + "'" +
            ", timestamp='" + getTimestamp() + "'" +
            ", dir='" + getStudyDir() + "'" +
            ", resources='" + getResources() + "'" +
            "}";
    }

}