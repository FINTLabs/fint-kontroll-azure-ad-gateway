package no.fintlabs.config;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigGroup {

    private static final List<String> groupAttributes = Arrays.asList (
            "id",
            "displayName"
    );

    private static final String membersAttribute = "members";

    //TODO: FKS-688 Change from fintkontrollidattribute to resourceGroupIDattribute. Needs change in 1Password secure note at the same time
    private String fintkontrollidattribute;
    private String prefix;
    private String suffix;
    private Boolean allowgroupupdate;
    private Boolean allowgroupdelete;
    private Integer grouppagingsize;

    public String[] getAllGroupAttributes() {
        List<String> allAttribs = new ArrayList<>(groupAttributes);
        allAttribs.add(membersAttribute);
        allAttribs.add(this.getFintkontrollidattribute());
        return allAttribs.toArray(new String[0]);
    };

    public String[] getGroupAttributesNotMembers() {
        List<String> allAttribs = new ArrayList<>(groupAttributes);
        allAttribs.add(this.getFintkontrollidattribute());
        return allAttribs.toArray(new String[0]);
    };
}