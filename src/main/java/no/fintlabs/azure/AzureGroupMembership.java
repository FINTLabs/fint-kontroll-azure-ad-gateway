package no.fintlabs.azure;

import com.microsoft.graph.models.DirectoryObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Log4j2
public class AzureGroupMembership {
    private String user_id;
    private String group_id;

    protected String id;

    public AzureGroupMembership(String group_id, DirectoryObject directoryObject) {
        this.group_id = group_id;
        this.user_id = directoryObject.getId();
        this.id = this.group_id + "_" + this.user_id;
    }
}