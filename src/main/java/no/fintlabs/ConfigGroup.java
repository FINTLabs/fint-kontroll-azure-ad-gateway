package no.fintlabs;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigGroup {

    //TODO: FKS-688 Change from fintkontrollidattribute to resourceGroupIDattribute. Needs change in 1Password secure note at the same time
    private String fintkontrollidattribute;
    private String prefix;
    private String suffix;
    private Boolean allowgroupupdate;
    private String uniquenameprefix;
}