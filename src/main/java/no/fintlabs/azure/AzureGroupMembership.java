package no.fintlabs.azure;

import com.microsoft.graph.models.DirectoryObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Getter
@Setter
@Log4j2
public class AzureGroupMembership {
    private String user_id;
    private String group_id;

    protected String id;

    public AzureGroupMembership(String group_id, DirectoryObject directoryObject) {
        this.group_id = group_id;
        this.user_id = directoryObject.id;
        this.id = this.group_id + "_" + this.user_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // If the references are the same
        if (o == null || getClass() != o.getClass()) return false; // Check type compatibility
        AzureGroupMembership azureGroupMembership = (AzureGroupMembership) o; // Cast and compare
        return Objects.equals(user_id, azureGroupMembership.user_id) &&
                Objects.equals(group_id, azureGroupMembership.group_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_id, group_id);
    }
}