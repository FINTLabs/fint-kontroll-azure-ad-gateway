package no.fintlabs.azure;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.GroupMembers;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.codec.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;

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
        //this.id = DigestUtils.sha3_512Hex(this.group_id + this.user_id);
        this.id = this.group_id + "_" + this.user_id;
    }
    /*public AzureGroupMembership(T membership) {
        this.id = membership.groupId;
        //this.user_id = group.
    }*/
}